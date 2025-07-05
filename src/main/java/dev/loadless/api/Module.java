package dev.loadless.api;

public interface Module {
    String getName();
    String getVersion();
    void onLoad();
    void onUnload();
}
