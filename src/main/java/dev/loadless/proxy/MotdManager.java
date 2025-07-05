package dev.loadless.proxy;

import dev.loadless.config.ConfigManager;

public class MotdManager {
    private final ConfigManager configManager;
    private static final String MOTD_KEY = "motd";
    private static final String DEFAULT_MOTD = "\u00A7aLoadless Proxy Server";

    public MotdManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public String getMotd() {
        return configManager.getModuleParam("loadless-core", MOTD_KEY, DEFAULT_MOTD);
    }

    public void setMotd(String motd) {
        try {
            configManager.setModuleParam("loadless-core", MOTD_KEY, motd);
        } catch (Exception e) {
            System.err.println("[MOTD] Ошибка сохранения MOTD: " + e.getMessage());
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
