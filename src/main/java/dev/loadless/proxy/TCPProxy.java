package dev.loadless.proxy;

import dev.loadless.core.Logger;
import java.io.*;
import java.net.*;

public class TCPProxy implements Runnable {
    private final int listenPort;
    private final String targetHost;
    private final int targetPort;
    private final Logger logger;
    private volatile boolean running = false;

    public TCPProxy(int listenPort, String targetHost, int targetPort, Logger logger) {
        this.listenPort = listenPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.logger = logger;
    }

    public void start() {
        running = true;
        new Thread(this, "TCPProxy-" + listenPort).start();
        logger.log("[TCPProxy] Прокси на порту " + listenPort + " -> " + targetHost + ":" + targetPort + " запущен");
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            while (running) {
                Socket client = serverSocket.accept();
                logger.log("[TCPProxy] Новое соединение: " + client.getRemoteSocketAddress());
                new Thread(() -> handleClient(client), "TCPProxy-Client").start();
            }
        } catch (IOException e) {
            logger.error("[TCPProxy] Ошибка сервера на порту " + listenPort + ": " + e.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (client; Socket server = new Socket(targetHost, targetPort)) {
            logger.log("[TCPProxy] Проксируем: " + client.getRemoteSocketAddress() + " <-> " + targetHost + ":" + targetPort);
            Thread t1 = new Thread(() -> {
                try {
                    forward(client.getInputStream(), server.getOutputStream());
                } catch (IOException e) {
                    logger.error("[TCPProxy] Ошибка потока client->server: " + e.getMessage());
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    forward(server.getInputStream(), client.getOutputStream());
                } catch (IOException e) {
                    logger.error("[TCPProxy] Ошибка потока server->client: " + e.getMessage());
                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            logger.log("[TCPProxy] Соединение завершено: " + client.getRemoteSocketAddress());
        } catch (Exception e) {
            logger.error("[TCPProxy] Ошибка проксирования: " + e.getMessage());
        }
    }

    private void forward(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
            out.flush();
        }
    }
}
