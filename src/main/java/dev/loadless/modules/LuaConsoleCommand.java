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
        if (onCommand.isnil() || !onCommand.isfunction()) {
            return "[Lua] В этом модуле не реализована обработка команд (onCommand отсутствует)";
        }
        LuaValue luaCmd = LuaValue.valueOf(name);
        org.luaj.vm2.LuaTable luaArgs = new org.luaj.vm2.LuaTable();
        for (int i = 0; i < args.length; i++) {
            luaArgs.set(i + 1, LuaValue.valueOf(args[i]));
        }
        try {
            org.luaj.vm2.Varargs result = onCommand.invoke(LuaValue.varargsOf(new LuaValue[]{luaCmd, luaArgs}));
            if (result.narg() > 0) {
                LuaValue v = result.arg1();
                if (v.isstring()) {
                    return v.tojstring();
                } else if (v.toboolean()) {
                    return null;
                }
            }
        } catch (Exception e) {
            return "[Lua] Ошибка выполнения команды: " + e.getMessage();
        }
        return "[Lua] Команда не реализована в onCommand";
    }
}
