package dev.loadless.core.command;

import dev.loadless.api.ConsoleCommand;
import dev.loadless.proxy.ProxyServer;
import dev.loadless.proxy.ProxyServer.ConnectedUser;
import java.util.Map;
import java.util.stream.Collectors;

public class ListUsersCommand implements ConsoleCommand {
    private final ProxyServer proxyServer;

    public ListUsersCommand(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Показать список подключённых игроков (ник, UUID, время входа)";
    }

    @Override
    public String execute(String[] args) {
        Map<String, ConnectedUser> users = proxyServer.getConnectedUsers();
        if (users.isEmpty()) return "Нет подключённых игроков.";
        return users.values().stream()
                .map(u -> String.format("%s (UUID: %s, с %s)", u.name, u.uuid, u.connectedAt))
                .collect(Collectors.joining("\n"));
    }
}
