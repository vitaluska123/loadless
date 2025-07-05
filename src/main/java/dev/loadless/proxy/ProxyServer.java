package dev.loadless.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import dev.loadless.core.Logger;

public class ProxyServer {
    private final InetSocketAddress bindAddress;
    private final MotdManager motdManager;
    private final Logger logger;
    private final String realHost;
    private final int realPort;
    private volatile boolean running = false;

    public ProxyServer(String host, int port, MotdManager motdManager, Logger logger, String realHost, int realPort) {
        this.bindAddress = new InetSocketAddress(host, port);
        this.motdManager = motdManager;
        this.logger = logger;
        this.realHost = realHost;
        this.realPort = realPort;
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

    private void handleClient(Socket client) {
        logger.log("[Proxy] Попытка запроса от " + client.getRemoteSocketAddress());
        try (client) {
            client.setSoTimeout(5000);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            // Читаем handshake
            int packetLen = readVarInt(in);
            byte[] handshake = in.readNBytes(packetLen);
            // Определяем state (последний байт handshake)
            int state = handshake[handshake.length - 1] & 0xFF;
            if (state == 1) { // status (ping)
                // Читаем следующий пакет (status request)
                int nextPacketLen = readVarInt(in);
                int packetId = in.read();
                if (packetId == 0x00) {
                    logger.log("[Proxy] Ping-запрос (MOTD) от " + client.getRemoteSocketAddress());
                    String motdJson = "{\"version\":{\"name\":\"Loadless\",\"protocol\":754},\"players\":{\"max\":100,\"online\":0},\"description\":{\"text\":\"" + motdManager.getMotd() + "\"}}";
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
            // Не ping — сразу проксируем handshake + всё остальное
            java.io.SequenceInputStream fullIn = new java.io.SequenceInputStream(
                new java.io.ByteArrayInputStream(encodeVarInt(packetLen, handshake)), in);
            proxyToRealServer(client, fullIn, out);
        } catch (Exception e) {
            logger.error("[Proxy] Ошибка клиента (" + client.getRemoteSocketAddress() + "): " + e.getMessage());
        }
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
            server.setSoTimeout(5000);
            logger.log("[Proxy] Проксируем к реальному серверу: " + realHost + ":" + realPort);
            OutputStream serverOut = server.getOutputStream();
            InputStream serverIn = server.getInputStream();
            // Просто пересылаем всё между клиентом и сервером
            Thread t1 = new Thread(() -> forward(clientIn, serverOut));
            Thread t2 = new Thread(() -> forward(serverIn, clientOut));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } catch (Exception e) {
            logger.error("[Proxy] Ошибка проксирования: " + e.getMessage());
        }
    }

    private void forward(InputStream in, OutputStream out) {
        byte[] buf = new byte[4096];
        int len;
        try {
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
        } catch (IOException ignored) {}
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
}
