package dev.loadless.core.command;

import dev.loadless.api.ConsoleCommand;
import dev.loadless.proxy.ProxyServer;
import dev.loadless.proxy.ProxyServer.ConnectedUser;
import java.io.IOException;
import java.util.Map;

public class KickUserCommand implements ConsoleCommand {
    private final ProxyServer proxyServer;

    public KickUserCommand(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Отключить игрока по нику или UUID: kick <ник|uuid> [причина]";
    }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) return "Использование: kick <ник|uuid> [причина]";
        String target = args[0];
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "Вы были отключены администратором.";
        Map<String, ConnectedUser> users = proxyServer.getConnectedUsers();
        ConnectedUser user = users.values().stream()
                .filter(u -> u.name.equalsIgnoreCase(target) || u.uuid.equalsIgnoreCase(target))
                .findFirst().orElse(null);
        if (user == null) return "Игрок не найден: " + target;
        try {
            user.socket.close();
            proxyServer.kickUser(user.name);
        } catch (IOException e) {
            return "Ошибка при отключении: " + e.getMessage();
        }
        return "Игрок отключён: " + user.name + " (UUID: " + user.uuid + ")";
    }
}
