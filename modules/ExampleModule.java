package dev.loadless.example;

import dev.loadless.api.Module;
import dev.loadless.api.ConsoleCommand;
import dev.loadless.core.ConsoleCommandManager;

public class ExampleModule implements Module {
    @Override
    public String getName() {
        return "ExampleModule";
    }
    @Override
    public void onLoad() {
        // Можно оставить пустым, если регистрация команд делается иначе
    }

    @Override
    public void onUnload() {
        // Освобождение ресурсов, если нужно
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
