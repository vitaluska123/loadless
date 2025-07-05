package dev.loadless.core;

import dev.loadless.api.ConsoleCommand;
import java.util.*;

public class ConsoleCommandManager {
    private final Map<String, ConsoleCommand> commands = new LinkedHashMap<>();

    public void register(ConsoleCommand command) {
        commands.put(command.getName().toLowerCase(Locale.ROOT), command);
    }

    public ConsoleCommand get(String name) {
        return commands.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<ConsoleCommand> getAll() {
        return commands.values();
    }

    public String execute(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return null;
        String name = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        ConsoleCommand cmd = get(name);
        if (cmd == null) return "Неизвестная команда: " + name;
        try {
            String result = cmd.execute(args);
            return result != null ? result : "";
        } catch (Exception e) {
            return "Ошибка выполнения команды: " + e.getMessage();
        }
    }
}
