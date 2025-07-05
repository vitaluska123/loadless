package dev.loadless.proxy;

import dev.loadless.config.ConfigManager;

public class MotdManager {
    private final ConfigManager configManager;
    private static final String DEFAULT_MOTD = "\u00A7aLoadless Proxy Server";

    public MotdManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public String getMotd() {
        String motd = configManager.getDefaultMotd();
        return motd != null ? motd : DEFAULT_MOTD;
    }

    public void setMotd(String motd) {
        // Можно реализовать сохранение, если потребуется
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
