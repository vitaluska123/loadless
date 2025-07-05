package dev.loadless.proxy;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer {
    private final InetSocketAddress bindAddress;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ProxyServer(String host, int port) {
        this.bindAddress = new InetSocketAddress(host, port);
    }

    public void start() {
        // TODO: Реализация приёма и обработки ping-запросов Minecraft
        System.out.println("[Proxy] Сервер запущен на " + bindAddress);
        // Здесь будет запуск netty или nio-сервера для обработки пакетов
    }

    public void stop() {
        executor.shutdownNow();
        System.out.println("[Proxy] Сервер остановлен");
    }
}
