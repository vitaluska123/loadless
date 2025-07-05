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
        try {
            // Проверка EULA до любой инициализации
            EulaManager eulaManager = new EulaManager();
            if (!eulaManager.checkOrCreateEula()) {
                System.out.println("Пожалуйста, примите условия EULA в файле eula.txt и перезапустите Loadless.");
                return;
            }
            // Инициализация логгера и остальных компонентов только после принятия EULA
            Logger logger = new Logger();
            logger.log("Loadless proxy server starting...");
            ModulesManager modulesManager = new ModulesManager();
            modulesManager.createModulesDir();
            ConfigManager configManager = new ConfigManager(logger);
            ModuleLoader moduleLoader = new ModuleLoader(new File("modules"), configManager);
            moduleLoader.loadModules();
            String host = configManager.getCoreHost();
            int port = configManager.getCorePort();
            MotdManager motdManager = new MotdManager(configManager);
            ProxyServer proxyServer = new ProxyServer(host, port, motdManager, logger);
            proxyServer.start();
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации: " + e.getMessage());
        }
    }
}
