package dev.loadless.manager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class EulaManager {
    private static final String EULA_FILE = "eula.txt";

    public boolean checkOrCreateEula() throws IOException {
        File eula = new File(EULA_FILE);
        if (!eula.exists()) {
            try (FileWriter writer = new FileWriter(eula)) {
                writer.write("# By changing the setting below to TRUE you are indicating your agreement to our EULA.\n");
                writer.write("# https://github.com/loadless-proxy/loadless/eula\n");
                writer.write("eula=false\n");
            }
            return false;
        }
        for (String line : Files.readAllLines(eula.toPath())) {
            if (line.trim().equalsIgnoreCase("eula=true")) {
                return true;
            }
        }
        return false;
    }
}
