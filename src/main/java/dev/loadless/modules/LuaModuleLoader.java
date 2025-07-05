package dev.loadless.modules;

import dev.loadless.api.LuaModule;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class LuaModuleLoader {
    private final File modulesDir;
    private final List<LuaModule> loadedModules = new ArrayList<>();

    public LuaModuleLoader(File modulesDir) {
        this.modulesDir = modulesDir;
    }

    public void loadModules() {
        loadedModules.clear();
        if (!modulesDir.exists() || !modulesDir.isDirectory()) return;
        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) return;
        for (File zipFile : files) {
            try (ZipFile zip = new ZipFile(zipFile)) {
                ZipEntry manifestEntry = zip.getEntry("manifest.json");
                ZipEntry mainLuaEntry = zip.getEntry("main.lua");
                if (manifestEntry == null || mainLuaEntry == null) {
                    System.err.println("[LuaModuleLoader] Пропущен модуль (нет manifest.json или main.lua): " + zipFile.getName());
                    continue;
                }
                // Чтение manifest.json
                String manifestJson;
                try (InputStream is = zip.getInputStream(manifestEntry)) {
                    manifestJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                JSONObject manifest = new JSONObject(manifestJson);
                String name = manifest.optString("name", zipFile.getName());
                String version = manifest.optString("version", "1.0.0");
                // Распаковка main.lua во временный файл
                Path tempLua = Files.createTempFile("loadless-lua-", ".lua");
                try (InputStream is = zip.getInputStream(mainLuaEntry)) {
                    Files.copy(is, tempLua, StandardCopyOption.REPLACE_EXISTING);
                }
                // TODO: Интеграция с LuaJ: запуск main.lua, регистрация API, вызов onLoad
                System.out.println("[LuaModuleLoader] Найден Lua-модуль: " + name + " v" + version + " (" + zipFile.getName() + ")");
                // После интеграции с LuaJ: создать LuaModule-обёртку и добавить в loadedModules
            } catch (IOException e) {
                System.err.println("[LuaModuleLoader] Ошибка при загрузке модуля: " + zipFile.getName());
                e.printStackTrace();
            }
        }
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
