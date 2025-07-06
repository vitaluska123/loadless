package dev.loadless.modules;

import dev.loadless.api.ConsoleCommand;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class LuaConsoleCommand implements ConsoleCommand {
    private final String name;
    private final String description;
    private final Globals globals;

    public LuaConsoleCommand(String name, String description, Globals globals) {
        this.name = name;
        this.description = description;
        this.globals = globals;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "Lua-модульная команда";
    }

    @Override
    public String execute(String[] args) {
        LuaValue onCommand = globals.get("onCommand");
        if (!onCommand.isnil()) {
            LuaValue luaCmd = LuaValue.valueOf(name);
            LuaValue luaArgs = org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(args);
            try {
                Varargs result = onCommand.invoke(LuaValue.varargsOf(new LuaValue[]{luaCmd, luaArgs}));
                if (result.narg() > 0 && result.arg1().toboolean()) {
                    return null; // handled in Lua
                }
            } catch (Exception e) {
                return "[Lua] Ошибка выполнения команды: " + e.getMessage();
            }
        }
        return "[Lua] Команда не реализована в onCommand";
    }
}
