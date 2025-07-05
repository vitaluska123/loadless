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
                List<String> commands = new ArrayList<>();
                if (manifest.has("commands")) {
                    var arr = manifest.getJSONArray("commands");
                    for (int i = 0; i < arr.length(); i++) {
                        commands.add(arr.getString(i));
                    }
                }
                // Распаковка main.lua во временный файл
                Path tempLua = Files.createTempFile("loadless-lua-", ".lua");
                try (InputStream is = zip.getInputStream(mainLuaEntry)) {
                    Files.copy(is, tempLua, StandardCopyOption.REPLACE_EXISTING);
                }
                // Интеграция с LuaJ: запуск main.lua, регистрация API, вызов onLoad
                try {
                    org.luaj.vm2.Globals globals = org.luaj.vm2.lib.jse.JsePlatform.standardGlobals();
                    org.luaj.vm2.LuaValue chunk = globals.loadfile(tempLua.toString());
                    chunk.call();
                    // Вызов onLoad, если определён
                    org.luaj.vm2.LuaValue onLoad = globals.get("onLoad");
                    if (!onLoad.isnil()) {
                        onLoad.call();
                        System.out.println("[LuaModuleLoader] Вызван onLoad() для " + name);
                    }
                } catch (Exception e) {
                    System.err.println("[LuaModuleLoader] Ошибка LuaJ в модуле: " + name + ": " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("[LuaModuleLoader] Найден Lua-модуль: " + name + " v" + version + " (" + zipFile.getName() + ")");
                if (!commands.isEmpty()) {
                    System.out.println("[LuaModuleLoader] Команды модуля: " + String.join(", ", commands));
                }
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

    // Получить список команд из manifest.json для всех Lua-модулей
    public List<String> getManifestCommands() {
        List<String> all = new ArrayList<>();
        if (!modulesDir.exists() || !modulesDir.isDirectory()) return all;
        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) return all;
        for (File zipFile : files) {
            try (ZipFile zip = new ZipFile(zipFile)) {
                ZipEntry manifestEntry = zip.getEntry("manifest.json");
                if (manifestEntry == null) continue;
                String manifestJson;
                try (InputStream is = zip.getInputStream(manifestEntry)) {
                    manifestJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                JSONObject manifest = new JSONObject(manifestJson);
                if (manifest.has("commands")) {
                    var arr = manifest.getJSONArray("commands");
                    for (int i = 0; i < arr.length(); i++) {
                        all.add(arr.getString(i));
                    }
                }
            } catch (Exception ignored) {}
        }
        return all;
    }
}
