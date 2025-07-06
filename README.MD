# Loadless

Loadless — это легковесный Minecraft-прокси, не требующий Velocity или BungeeCord. Позволяет динамически менять MOTD, favicon, проксировать игроков и расширяться через Lua-модули.

## Возможности
- **Динамический MOTD** и favicon (server-icon.png) через config.xml и HTTP API (в разработке)
- **Прозрачное проксирование**: игроки могут подключаться и играть через Loadless
- **Модульность**: поддержка Lua-модулей (ZIP-архивы и папки), изоляция, API для расширения
- **Гибкая настройка**: адреса и порты прокси и реального сервера настраиваются через config.xml
- **Лёгкость**: не требует тяжёлых решений, минимальные зависимости, быстрый запуск
- **Подробное логирование**: все события пишутся в logs/latest.log и по дате
- **EULA**: запуск только после принятия eula.txt

## Быстрый старт

### Требования
- Java 17–24
- Gradle (или используйте wrapper)

### Сборка и запуск
```sh
# Сборка fat JAR
./gradlew shadowJar

# Запуск fat JAR (из папки run)
cd run
java -jar loadless-1.0.0.jar
```

## Модульность
- Lua-модули: ZIP-архивы или папки с manifest.json и main.lua в папке modules
- Модули изолированы, доступ к настройкам только через API
- Java-модули не поддерживаются (см. историю проекта)

## Прокси-сервер
- Принимает ping-запросы Minecraft, возвращает кастомный MOTD и favicon
- Проксирует игровые соединения к реальному серверу (игроки могут играть)
- Все параметры (адреса, порты, MOTD) настраиваются через config.xml

## Безопасность
- HTTP API для управления статусом будет защищён секретным ключом (в разработке)

## Планы
- [x] Прозрачное проксирование и MOTD
- [x] Favicon (server-icon.png)
- [x] Модульность (Lua ZIP/папки)
- [ ] HTTP API для управления статусом
- [x] Примеры Lua-модулей и документация
- [ ] Юнит-тесты ядра

## Как подключить Lua-модуль
1. Создайте папку или ZIP-архив с двумя файлами: `manifest.json` и `main.lua`.
2. Пример manifest.json:
```json
{
  "name": "HelloLua",
  "version": "1.0.0",
  "author": "yourname",
  "description": "Пример Lua-модуля для Loadless",
  "commands": [
    { "name": "hello-lua", "description": "Приветствие из Lua-модуля" },
    { "name": "lua-time", "description": "Показать текущее время" },
    { "name": "lua-echo", "description": "Повторить аргументы" }
  ]
}
```
3. Пример main.lua:
```lua
function onLoad()
    print("[HelloLua] Lua-модуль загружен!")
end

function onUnload()
    print("[HelloLua] Lua-модуль выгружен!")
end

function onCommand(cmd, args)
    if cmd == "hello-lua" then
        return "Привет из Lua-модуля!"
    elseif cmd == "lua-time" then
        return "Текущее время: " .. os.date("%Y-%m-%d %H:%M:%S")
    elseif cmd == "lua-echo" then
        return table.concat(args, " ")
    end
    return false
end
```
4. Поместите папку или ZIP-модуль в папку `modules` в директории Loadless.
5. Перезапустите Loadless — модуль будет автоматически загружен, команды появятся в help и будут работать в консоли.

## Структура Lua-модуля
- `manifest.json` — описание модуля и команд (name, description, version, commands)
- `main.lua` — основной скрипт, реализует функции onLoad, onUnload, onCommand
- Все команды, указанные в manifest.json, автоматически регистрируются в консоли
- Аргументы команд передаются в onCommand как Lua-таблица

## Middleware (обработчики событий)
- Lua-модули могут объявлять middleware-обработчики для событий ядра (например, onPing, onLogin, onPacket и др.)
- В manifest.json добавьте поле `middleware`:
```json
"middleware": [
  { "event": "onPing", "handler": "myMotdHandler", "priority": 10 },
  { "event": "onPing", "handler": "myOtherHandler", "priority": 20 }
]
```
- В main.lua реализуйте функции-обработчики:
```lua
function myMotdHandler(event)
    event.motd = "MOTD изменён через middleware!"
end
```
- Приоритет (priority) определяет порядок вызова (чем меньше — тем раньше). Если веса совпадают, порядок не гарантируется.
- Middleware могут изменять данные события (motd, favicon, kick и др.)

---

**Loadless** — лёгкое решение для управления статусом и проксированием Minecraft-сервера без лишних сложностей!
