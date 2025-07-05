# Пример Lua-модуля для Loadless

## Структура Lua-модуля
Lua-модуль — это ZIP-архив, который кладётся в папку `modules`. В архиве должны быть:
- `main.lua` — основной скрипт
- `manifest.json` — описание модуля (имя, версия, автор и т.д.)

**Пример структуры архива:**
```
my-lua-module.zip
├── main.lua
└── manifest.json
```

## Пример manifest.json
```json
{
  "name": "HelloLua",
  "version": "1.0.0",
  "author": "yourname",
  "description": "Пример Lua-модуля для Loadless"
}
```

## Пример main.lua
```lua
function onLoad()
    print("[HelloLua] Lua-модуль загружен!")
end

function onUnload()
    print("[HelloLua] Lua-модуль выгружен!")
end

function onCommand(cmd, args)
    if cmd == "hello-lua" then
        print("Привет из Lua-модуля!")
        return true
    end
    return false
end
```

## Как подключить Lua-модуль
1. Создайте main.lua и manifest.json, упакуйте их в ZIP-архив (например, my-lua-module.zip).
2. Поместите архив в папку `modules`.
3. Перезапустите Loadless — модуль будет автоматически загружен.

## API Lua-модуля (минимум)
- `onLoad()` — вызывается при загрузке модуля
- `onUnload()` — вызывается при выгрузке
- `onCommand(cmd, args)` — вызывается при вводе команды в консоль (если реализовано)

---

Lua-модули не требуют компиляции и могут быть быстро протестированы и обновлены!
