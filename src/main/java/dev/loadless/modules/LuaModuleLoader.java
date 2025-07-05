package dev.loadless.modules;

import dev.loadless.api.LuaModule;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LuaModuleLoader {
    private final File modulesDir;
    private final List<LuaModule> loadedModules = new ArrayList<>();

    public LuaModuleLoader(File modulesDir) {
        this.modulesDir = modulesDir;
    }

    public void loadModules() {
        // TODO: Реализация загрузки Lua-модулей из ZIP-архивов
        // 1. Найти все .zip в modulesDir
        // 2. Проверить структуру архива
        // 3. Подключить через LuaJ или аналогичный движок
        // 4. Зарегистрировать модули
    }

    public void unloadModules() {
        for (LuaModule module : loadedModules) {
            try {
                module.onUnload();
            } catch (Exception ignored) {}
        }
        loadedModules.clear();
    }

    public List<LuaModule> getLoadedModules() {
        return List.copyOf(loadedModules);
    }
}
