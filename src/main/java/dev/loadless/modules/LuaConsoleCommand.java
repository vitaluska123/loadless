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
                org.luaj.vm2.Varargs result = onCommand.invoke(LuaValue.varargsOf(new LuaValue[]{luaCmd, luaArgs}));
                if (result.narg() > 0) {
                    LuaValue v = result.arg1();
                    if (v.isstring()) {
                        return v.tojstring(); // если onCommand вернул строку — вывести её
                    } else if (v.toboolean()) {
                        return null; // true — команда обработана, но ничего не выводить
                    }
                }
            } catch (Exception e) {
                return "[Lua] Ошибка выполнения команды: " + e.getMessage();
            }
        }
        return "[Lua] Команда не реализована в onCommand";
    }
}
