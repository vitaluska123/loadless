package dev.loadless.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModulesManager {
    private static final String MODULES_DIR = "modules";

    public void createModulesDir() throws IOException {
        Path modulesPath = Path.of(MODULES_DIR);
        if (!Files.exists(modulesPath)) {
            Files.createDirectory(modulesPath);
            System.out.println("Создана папка modules для модулей.");
        }
    }
}
