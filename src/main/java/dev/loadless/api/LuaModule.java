package dev.loadless.api;

public interface LuaModule {
    String getName();
    String getVersion();
    void onLoad();
    void onUnload();
}
