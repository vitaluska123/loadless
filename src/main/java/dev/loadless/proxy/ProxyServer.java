package dev.loadless.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import dev.loadless.core.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import dev.loadless.config.ConfigManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.Instant;

public class ProxyServer {
    private final InetSocketAddress bindAddress;
    private final MotdManager motdManager;
    private final Logger logger;
    private final String realHost;
    private final int realPort;
    private final ConfigManager configManager;
    private volatile boolean running = false;

    // Потокобезопасный список подключившихся пользователей
    private final ConcurrentHashMap<String, ConnectedUser> connectedUsers = new ConcurrentHashMap<>();

    public ProxyServer(String host, int port, MotdManager motdManager, Logger logger, String realHost, int realPort, ConfigManager configManager) {
        this.bindAddress = new InetSocketAddress(host, port);
        this.motdManager = motdManager;
        this.logger = logger;
        this.realHost = realHost;
        this.realPort = realPort;
        this.configManager = configManager;
    }

    public void start() {
        testRealServerConnection();
        running = true;
        new Thread(this::runServer, "Loadless-Proxy-Main").start();
        logger.log("[Proxy] Сервер запущен на " + bindAddress);
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(bindAddress);
            while (running) {
                Socket client = serverSocket.accept();
                logger.log("[Proxy] Принято новое TCP-соединение: " + client.getRemoteSocketAddress());
                logger.log("[Proxy] Новое подключение: " + client.getRemoteSocketAddress());
                new Thread(() -> handleClient(client), "Loadless-Proxy-Client").start();
            }
        } catch (IOException e) {
            logger.error("[Proxy] Ошибка сервера: " + e.getMessage());
        }
    }

    // Кэшированная строка favicon
    private String cachedFaviconBase64 = null;
    private long faviconLastLoaded = 0;
    private static final long FAVICON_RELOAD_INTERVAL_MS = 60_000; // 1 минута

    private String getFaviconBase64() {
        try {
            // Если уже кэшировано и не истекло время — возвращаем кэш
            if (cachedFaviconBase64 != null && (System.currentTimeMillis() - faviconLastLoaded) < FAVICON_RELOAD_INTERVAL_MS) {
                return cachedFaviconBase64;
            }
            Path iconPath = Path.of("server-icon.png");
            if (Files.exists(iconPath)) {
                byte[] iconBytes = Files.readAllBytes(iconPath);
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(iconPath.toFile());
                if (img == null) {
                    logger.log("[Proxy] server-icon.png не читается как PNG!");
                    return null;
                }
                if (img.getWidth() != 64 || img.getHeight() != 64) {
                    logger.log("[Proxy] server-icon.png должен быть PNG 64x64! Сейчас: " + img.getWidth() + "x" + img.getHeight());
                    return null;
                }
                String base64 = Base64.getEncoder().encodeToString(iconBytes);
                cachedFaviconBase64 = "data:image/png;base64," + base64;
                faviconLastLoaded = System.currentTimeMillis();
                logger.log("[Proxy] Favicon успешно найден и закэширован.");
                return cachedFaviconBase64;
            } else {
                logger.log("[Proxy] server-icon.png не найден в рабочей директории!");
                cachedFaviconBase64 = null;
            }
        } catch (Exception e) {
            logger.log("[Proxy] Ошибка чтения server-icon.png: " + e.getMessage());
        }
        return null;
    }

    // Для хранения последней ошибки getRealServerPlayers
    private String lastGetRealServerPlayersError = null;

    private int[] getRealServerPlayers() {
        // Возвращает [online, max] или null если не удалось
        lastGetRealServerPlayersError = null;
        try (Socket server = new Socket(realHost, realPort)) {
            server.setSoTimeout(2000);
            OutputStream out = server.getOutputStream();
            InputStream in = server.getInputStream();
            // Handshake + Status request
            java.io.ByteArrayOutputStream handshake = new java.io.ByteArrayOutputStream();
            handshake.write(0x00); // packet id
            // Protocol version (use config value)
            writeVarInt(handshake, configManager.getVersionProtocol());
            byte[] hostBytes = realHost.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            writeVarInt(handshake, hostBytes.length);
            handshake.write(hostBytes);
            handshake.write((realPort >> 8) & 0xFF);
            handshake.write(realPort & 0xFF);
            handshake.write(1); // next state: status
            byte[] handshakeBytes = handshake.toByteArray();
            writeVarInt(out, handshakeBytes.length);
            out.write(handshakeBytes);
            // Status request
            out.write(0x01); out.write(0x00);
            out.flush();
            // Read response
            readVarInt(in); // packet length
            int packetId = readVarInt(in);
            if (packetId != 0x00) return null;
            int strLen = readVarInt(in);
            byte[] strBytes = in.readNBytes(strLen);
            String json = new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
            int online = -1, max = -1;
            // Новый парсер: ищем число до первой нецифры
            online = extractJsonInt(json, "\"online\":");
            max = extractJsonInt(json, "\"max\":");
            if (online >= 0 && max >= 0) return new int[]{online, max};
        } catch (Exception e) {
            lastGetRealServerPlayersError = e.getMessage();
            logger.log("[Proxy] Не удалось получить онлайн с реального сервера: " + e.getMessage());
        }
        return null;
    }

    private int extractJsonInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return -1;
        idx += key.length();
        int end = idx;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end > idx) {
            try { return Integer.parseInt(json.substring(idx, end)); } catch (Exception ignored) {}
        }
        return -1;
    }

    // Класс для хранения информации о подключённом пользователе
    public static class ConnectedUser {
        public final String name;
        public final String uuid;
        public final Socket socket;
        public final Instant connectedAt;
        public ConnectedUser(String name, String uuid, Socket socket) {
            this.name = name;
            this.uuid = uuid;
            this.socket = socket;
            this.connectedAt = Instant.now();
        }
    }

    private void handleClient(Socket client) {
        logger.log("[Proxy] Попытка запроса от " + client.getRemoteSocketAddress());
        String userName = null;
        String userUuid = null;
        try (client) {
            // Увеличиваем таймаут ожидания для долгой прогрузки (например, 60 секунд)
            client.setSoTimeout(60000);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            // Читаем handshake
            int packetLen = readVarInt(in);
            byte[] handshake = in.readNBytes(packetLen);
            // Определяем state (последний байт handshake)
            int state = handshake[handshake.length - 1] & 0xFF;
            if (state == 1) { // status (ping)
                // Читаем следующий пакет (status request)
                @SuppressWarnings("unused")
                int nextPacketLen = readVarInt(in); // ОБЯЗАТЕЛЬНО читать VarInt длины!
                
                int packetId = in.read();
                if (packetId == 0x00) {
                    logger.log("[Proxy] Ping-запрос (MOTD) от " + client.getRemoteSocketAddress());
                    // --- MOTD и онлайн всегда кастомные ---
                    int playersOnline, playersMax;
                    int[] real = getRealServerPlayers();
                    if (real != null) {
                        playersOnline = real[0];
                        playersMax = real[1];
                    } else {
                        playersOnline = configManager.getPlayersOnline();
                        playersMax = configManager.getPlayersMax();
                    }
                    String playersSample = "[]";
                    String versionBlock;
                    if (lastGetRealServerPlayersError != null && lastGetRealServerPlayersError.contains("Connection refused")) {
                        String offlineFlag = configManager.getOfflineFlag();
                        versionBlock = "\"version\":{\"name\":\"" + offlineFlag + "\",\"protocol\":999},";
                    } else {
                        versionBlock = "\"version\":{\"name\":\"" + configManager.getVersionName() + "\",\"protocol\":" + configManager.getVersionProtocol() + "},";
                    }
                    String favicon = getFaviconBase64();
                    String motdJson;
                    if (favicon != null) {
                        String safeFavicon = favicon.replace("\\", "\\\\").replace("\"", "\\\"");
                        motdJson = "{" +
                                versionBlock +
                                "\"players\":{\"max\":" + playersMax + ",\"online\":" + playersOnline + ",\"sample\":" + playersSample + "}," +
                                "\"description\":{\"text\":\"" + motdManager.getMotd() + "\"}," +
                                "\"favicon\":\"" + safeFavicon + "\"}";
                    } else {
                        motdJson = "{" +
                                versionBlock +
                                "\"players\":{\"max\":" + playersMax + ",\"online\":" + playersOnline + ",\"sample\":" + playersSample + "}," +
                                "\"description\":{\"text\":\"" + motdManager.getMotd() + "\"}" +
                                "}";
                    }
                    byte[] response = createStatusResponse(motdJson);
                    out.write(response);
                    out.flush();
                    // Попытка прочитать ping (0x01), если есть, с коротким таймаутом
                    try {
                        client.setSoTimeout(300); // короткий таймаут (300 мс)
                        int pingLen = readVarInt(in);
                        byte[] pingPacket = in.readNBytes(pingLen);
                        out.write(createPingResponse(pingPacket));
                        out.flush();
                    } catch (Exception ignored) {
                        // Если ping не пришёл — это нормально, просто закрываем соединение
                    }
                    return;
                }
            }
            // --- Логирование входа игрока ---
            int loginLen = readVarInt(in);
            byte[] loginPacket = in.readNBytes(loginLen);
            if (loginPacket.length > 0 && loginPacket[0] == 0x00) {
                int offset = 1;
                int nameLen = 0;
                int shift = 0;
                do {
                    nameLen |= (loginPacket[offset] & 0x7F) << shift;
                    shift += 7;
                } while ((loginPacket[offset++] & 0x80) != 0);
                String name = new String(loginPacket, offset, nameLen, java.nio.charset.StandardCharsets.UTF_8);
                offset += nameLen;
                String uuid = "-";
                if (loginPacket.length >= offset + 16) {
                    byte[] uuidBytes = new byte[16];
                    System.arraycopy(loginPacket, offset, uuidBytes, 0, 16);
                    uuid = bytesToHex(uuidBytes);
                }
                logger.log("[Proxy] Игрок подключился: " + name + " (UUID: " + uuid + ")");
                userName = name;
                userUuid = uuid;
                // Добавляем пользователя в список
                connectedUsers.put(name, new ConnectedUser(name, uuid, client));
            }
            // Не ping — проксируем handshake + login start + всё остальное
            InputStream fullIn = new java.io.SequenceInputStream(
                new java.io.SequenceInputStream(
                    new java.io.ByteArrayInputStream(encodeVarInt(packetLen, handshake)),
                    new java.io.ByteArrayInputStream(encodeVarInt(loginLen, loginPacket))
                ),
                in
            );
            proxyToRealServer(client, fullIn, out);
        } catch (Exception e) {
            logger.error("[Proxy] Ошибка клиента (" + client.getRemoteSocketAddress() + "): " + e.getMessage());
        } finally {
            // Удаляем пользователя из списка при отключении
            if (userName != null) {
                connectedUsers.remove(userName);
                logger.log("[Proxy] Игрок отключился: " + userName + (userUuid != null ? " (UUID: " + userUuid + ")" : ""));
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] encodeVarInt(int len, byte[] data) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            int value = len;
            do {
                byte temp = (byte) (value & 0b01111111);
                value >>>= 7;
                if (value != 0) temp |= 0b10000000;
                out.write(temp);
            } while (value != 0);
            out.write(data);
        } catch (Exception ignored) {}
        return out.toByteArray();
    }

    private void proxyToRealServer(Socket client, InputStream clientIn, OutputStream clientOut) {
        try (Socket server = new Socket(realHost, realPort)) {
            // Увеличиваем таймауты для обеих сторон (например, 60 секунд)
            server.setSoTimeout(60000);
            client.setSoTimeout(60000);
            logger.log("[Proxy] Проксируем к реальному серверу: " + realHost + ":" + realPort);
            OutputStream serverOut = server.getOutputStream();
            InputStream serverIn = server.getInputStream();
            Thread t1 = new Thread(() -> forward(clientIn, serverOut, "client->server"));
            Thread t2 = new Thread(() -> forward(serverIn, clientOut, "server->client"));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            logger.log("[Proxy] Проксирование завершено для " + client.getRemoteSocketAddress());
        } catch (Exception e) {
            logger.error("[Proxy] Ошибка проксирования: " + e.getMessage());
        }
    }

    private void forward(InputStream in, OutputStream out, String direction) {
        byte[] buf = new byte[4096];
        int len;
        try {
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
            logger.log("[Proxy] Поток завершён: " + direction);
        } catch (IOException e) {
            logger.log("[Proxy] Обрыв потока (" + direction + "): " + e.getMessage());
        }
    }

    private int readVarInt(InputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = (byte) in.read();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) throw new IOException("VarInt слишком длинный");
        } while ((read & 0b10000000) != 0);
        return result;
    }

    private void writeVarInt(OutputStream out, int value) throws IOException {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) temp |= 0b10000000;
            out.write(temp);
        } while (value != 0);
    }

    private byte[] createStatusResponse(String json) throws IOException {
        byte[] jsonBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        data.write(0x00); // packet id
        writeVarInt(data, jsonBytes.length);
        data.write(jsonBytes);
        byte[] dataBytes = data.toByteArray();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        writeVarInt(out, dataBytes.length);
        out.write(dataBytes);
        return out.toByteArray();
    }

    private byte[] createPingResponse(byte[] pingPacket) {
        int len = 1 + (pingPacket.length - 1);
        byte[] packet = new byte[len + 5];
        int idx = 0;
        idx += writeVarIntToArray(packet, idx, len);
        packet[idx++] = 0x01; // packet id
        System.arraycopy(pingPacket, 1, packet, idx, pingPacket.length - 1);
        return packet;
    }

    private int writeVarIntToArray(byte[] arr, int offset, int value) {
        int start = offset;
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) temp |= 0b10000000;
            arr[offset++] = temp;
        } while (value != 0);
        return offset - start;
    }

    private void testRealServerConnection() {
        try (Socket testSocket = new Socket(realHost, realPort)) {
            logger.log("[Proxy] Тестовое соединение с реальным сервером успешно: " + realHost + ":" + realPort);
        } catch (IOException e) {
            logger.error("[Proxy] Не удалось подключиться к реальному серверу: " + realHost + ":" + realPort + " — " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        logger.log("[Proxy] Сервер остановлен");
    }

    // Метод для получения копии списка пользователей (например, для команды list)
    public Map<String, ConnectedUser> getConnectedUsers() {
        return new java.util.HashMap<>(connectedUsers);
    }

    // Отключение пользователя по нику (или UUID)
    public boolean kickUser(String nameOrUuid) {
        ConnectedUser user = connectedUsers.values().stream()
                .filter(u -> u.name.equalsIgnoreCase(nameOrUuid) || u.uuid.equalsIgnoreCase(nameOrUuid))
                .findFirst().orElse(null);
        if (user != null) {
            try {
                user.socket.close();
            } catch (Exception ignored) {}
            connectedUsers.remove(user.name);
            logger.log("[Proxy] Игрок был отключён через kick: " + user.name + " (UUID: " + user.uuid + ")");
            return true;
        }
        return false;
    }
}
