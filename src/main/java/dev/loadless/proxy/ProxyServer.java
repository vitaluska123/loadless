package dev.loadless.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;

public class ProxyServer {
    private final InetSocketAddress bindAddress;
    private final MotdManager motdManager;
    private volatile boolean running = false;

    public ProxyServer(String host, int port, MotdManager motdManager) {
        this.bindAddress = new InetSocketAddress(host, port);
        this.motdManager = motdManager;
    }

    public void start() {
        running = true;
        new Thread(this::runServer, "Loadless-Proxy-Main").start();
        System.out.println("[Proxy] Сервер запущен на " + bindAddress);
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(bindAddress);
            while (running) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client), "Loadless-Proxy-Client").start();
            }
        } catch (IOException e) {
            System.err.println("[Proxy] Ошибка сервера: " + e.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (client) {
            client.setSoTimeout(5000);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            // Читаем первый пакет (handshake)
            int packetLen = readVarInt(in);
            byte[] handshake = in.readNBytes(packetLen);
            int nextPacketLen = readVarInt(in);
            in.mark(1);
            int packetId = in.read();
            if (packetId == 0x00) { // status request
                // Отвечаем кастомным MOTD
                String motdJson = "{\"version\":{\"name\":\"Loadless\",\"protocol\":754},\"players\":{\"max\":100,\"online\":0},\"description\":{\"text\":\"" + motdManager.getMotd() + "\"}}";
                byte[] response = createStatusResponse(motdJson);
                out.write(response);
                out.flush();
                // Ждём ping (0x01) и отвечаем echo
                int pingLen = readVarInt(in);
                byte[] pingPacket = in.readNBytes(pingLen);
                out.write(createPingResponse(pingPacket));
                out.flush();
                return;
            } else {
                // Не ping — проксируем к реальному серверу
                in.reset();
                proxyToRealServer(client, handshake, in, out);
            }
        } catch (Exception e) {
            // Игнорируем ошибки соединения
        }
    }

    private void proxyToRealServer(Socket client, byte[] handshake, InputStream clientIn, OutputStream clientOut) {
        String realHost = motdManager.getConfigManager().getModuleParam("loadless-core", "realServerHost", "127.0.0.1");
        int realPort = Integer.parseInt(motdManager.getConfigManager().getModuleParam("loadless-core", "realServerPort", "25566"));
        try (Socket server = new Socket(realHost, realPort)) {
            server.setSoTimeout(5000);
            OutputStream serverOut = server.getOutputStream();
            InputStream serverIn = server.getInputStream();
            // Пересылаем handshake
            writeVarInt(serverOut, handshake.length);
            serverOut.write(handshake);
            serverOut.flush();
            // Два потока: клиент->сервер и сервер->клиент
            Thread t1 = new Thread(() -> forward(clientIn, serverOut));
            Thread t2 = new Thread(() -> forward(serverIn, clientOut));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } catch (Exception e) {
            // Игнорируем ошибки соединения
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
        byte[] jsonBytes = json.getBytes("UTF-8");
        int len = 1 + jsonBytes.length;
        byte[] packet = new byte[len + 5];
        int idx = 0;
        idx += writeVarIntToArray(packet, idx, len);
        packet[idx++] = 0x00; // packet id
        idx += writeVarIntToArray(packet, idx, jsonBytes.length);
        System.arraycopy(jsonBytes, 0, packet, idx, jsonBytes.length);
        return packet;
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

    public void stop() {
        running = false;
        System.out.println("[Proxy] Сервер остановлен");
    }
}
