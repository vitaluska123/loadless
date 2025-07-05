package dev.loadless.core;

import dev.loadless.config.ConfigManager;
import dev.loadless.manager.EulaManager;
import dev.loadless.manager.ModulesManager;
import dev.loadless.modules.ModuleLoader;
import dev.loadless.proxy.ProxyServer;
import dev.loadless.proxy.MotdManager;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        System.out.println("Loadless proxy server starting...");
        // Инициализация менеджеров
        EulaManager eulaManager = new EulaManager();
        ModulesManager modulesManager = new ModulesManager();
        try {
            // Инициализация config.xml
            ConfigManager configManager = new ConfigManager();
            // Используем менеджеры для инициализации (чтобы не было предупреждений о неиспользуемых переменных)
            if (!eulaManager.checkOrCreateEula()) {
                System.out.println("Пожалуйста, примите условия EULA в файле eula.txt и перезапустите Loadless.");
                return;
            }
            modulesManager.createModulesDir();
            // Загрузка Java-модулей с поддержкой config.xml
            ModuleLoader moduleLoader = new ModuleLoader(new File("modules"), configManager);
            moduleLoader.loadModules();
            // Запуск прокси-сервера с параметрами из config.xml
            String host = configManager.getCoreHost();
            int port = configManager.getCorePort();
            MotdManager motdManager = new MotdManager(configManager);
            ProxyServer proxyServer = new ProxyServer(host, port, motdManager);
            proxyServer.start();
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации: " + e.getMessage());
        }
    }
}
