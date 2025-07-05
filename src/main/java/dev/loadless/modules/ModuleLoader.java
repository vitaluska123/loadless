package dev.loadless.modules;

import dev.loadless.api.Module;
import dev.loadless.config.ConfigManager;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ModuleLoader {
    private final File modulesDir;
    private final List<Module> loadedModules = new ArrayList<>();
    private ConfigManager configManager;

    public ModuleLoader(File modulesDir, ConfigManager configManager) {
        this.modulesDir = modulesDir;
        this.configManager = configManager;
    }

    public void loadModules() {
        if (!modulesDir.exists() || !modulesDir.isDirectory()) return;
        File[] jars = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return;
        for (File jar : jars) {
            try {
                URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, this.getClass().getClassLoader());
                ServiceLoader<Module> serviceLoader = ServiceLoader.load(Module.class, cl);
                for (Module module : serviceLoader) {
                    // Передаём конфиг в модуль через API (если потребуется)
                    configManager.getOrCreateModuleConfig(module.getName());
                    module.onLoad();
                    loadedModules.add(module);
                    System.out.println("Загружен модуль: " + module.getName() + " v" + module.getVersion());
                }
            } catch (Exception e) {
                System.err.println("Ошибка загрузки модуля из " + jar.getName() + ": " + e.getMessage());
            }
        }
    }

    public void unloadModules() {
        for (Module module : loadedModules) {
            try {
                module.onUnload();
            } catch (Exception ignored) {}
        }
        loadedModules.clear();
    }

    public List<Module> getLoadedModules() {
        return List.copyOf(loadedModules);
    }
}
