package dev.loadless.core;

import dev.loadless.api.ConsoleCommand;
import dev.loadless.config.ConfigManager;
import dev.loadless.manager.EulaManager;
import dev.loadless.manager.ModulesManager;
import dev.loadless.modules.LuaModuleLoader;
import dev.loadless.proxy.ProxyServer;
import dev.loadless.proxy.MotdManager;
import dev.loadless.core.command.ListUsersCommand;
import dev.loadless.core.command.KickUserCommand;

import java.io.File;
import java.util.Scanner;

public class Main {
    private static ConsoleCommandManager staticCmdManager;
    public static void registerLuaCommand(ConsoleCommand cmd) {
        if (staticCmdManager != null) {
            staticCmdManager.register(cmd);
        }
    }

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
            logger.log("[Core] Loadless proxy server starting...");
            try{
                ModulesManager modulesManager = new ModulesManager();
                modulesManager.createModulesDir();
                ConfigManager configManager = new ConfigManager(logger);
                LuaModuleLoader luaModuleLoader = new LuaModuleLoader(new File("modules"));
                luaModuleLoader.loadModules();
                String host = configManager.getCoreHost();
                int port = configManager.getCorePort();
                String realHost = configManager.getRealServerHost();
                int realPort = configManager.getRealServerPort();
                MotdManager motdManager = new MotdManager(configManager);
                ProxyServer proxyServer = new ProxyServer(host, port, motdManager, logger, realHost, realPort, configManager);
                proxyServer.start();

                // --- Console commands ---
                ConsoleCommandManager cmdManager = new ConsoleCommandManager();
                staticCmdManager = cmdManager;
                // help
                cmdManager.register(new ConsoleCommand() {
                    public String getName() { return "help"; }
                    public String getDescription() { return "Показать список команд"; }
                    public String execute(String[] args) {
                        StringBuilder sb = new StringBuilder("Доступные команды:\n");
                        for (ConsoleCommand c : cmdManager.getAll()) {
                            sb.append(c.getName()).append(" - ").append(c.getDescription()).append("\n");
                        }
                        // Выводим команды Lua-модулей с описаниями из manifest.json
                        var luaCmds = luaModuleLoader.getManifestCommandDescriptions();
                        if (!luaCmds.isEmpty()) {
                            sb.append("\n[Lua-модули]\n");
                            for (String cmd : luaCmds) {
                                sb.append(cmd).append("\n");
                            }
                        }
                        return sb.toString();
                    }
                });
                // quit
                cmdManager.register(new ConsoleCommand() {
                    public String getName() { return "quit"; }
                    public String getDescription() { return "Завершить работу прокси"; }
                    public String execute(String[] args) {
                        logger.log("[Core] Завершение работы по команде quit");
                        System.exit(0);
                        return null;
                    }
                });
                // list
                cmdManager.register(new ListUsersCommand(proxyServer));
                // kick
                cmdManager.register(new KickUserCommand(proxyServer));
                // Регистрация команд от Lua-модулей
                for (var module : luaModuleLoader.getLoadedModules()) {
                    if (module instanceof dev.loadless.api.LuaModule) {
                        if (module instanceof dev.loadless.api.ConsoleCommand) {
                            cmdManager.register((dev.loadless.api.ConsoleCommand) module);
                        }
                    }
                }

                // Поток для чтения команд
                new Thread(() -> {
                    Scanner scanner = new Scanner(System.in);
                    while (true) {
                        if (!scanner.hasNextLine()) break;
                        String line = scanner.nextLine();
                        if (line == null) break;
                        String result = cmdManager.execute(line);
                        if (result != null && !result.isEmpty()) {
                            System.out.println(result);
                        }
                    }
                }, "Console-Input").start();
            } catch (Exception e) {
                logger.log("[Core] Ошибка при инициализации: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации: " + e.getMessage());
        }
    }
}
