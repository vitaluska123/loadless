# Пример Java-модуля для Loadless

## Минимальный модуль

```java
package dev.loadless.example;

import dev.loadless.api.Module;
import dev.loadless.api.ConsoleCommand;
import dev.loadless.core.ConsoleCommandManager;

public class ExampleModule implements Module {
    @Override
    public String getName() {
        return "ExampleModule";
    }

    @Override
    public void onLoad(ConsoleCommandManager commandManager) {
        // Регистрируем простую команду hello
        commandManager.register(new ConsoleCommand() {
            @Override
            public String getName() { return "hello"; }
            @Override
            public String getDescription() { return "Пример команды из модуля"; }
            @Override
            public String execute(String[] args) {
                return "Привет из ExampleModule!";
            }
        });
    }
}
```

## Как подключить модуль
1. Скомпилируйте класс ExampleModule в JAR (или несколько классов, если модуль сложнее).
2. Поместите JAR-файл в папку `modules` в директории Loadless.
3. Перезапустите Loadless — модуль будет автоматически загружен, команда появится в консоли.

## Структура Java-модуля
- Класс должен реализовывать интерфейс `dev.loadless.api.Module`.
- Метод `onLoad(ConsoleCommandManager)` вызывается при загрузке модуля.
- Для регистрации консольных команд используйте `commandManager.register(...)`.
- Можно использовать любые зависимости, если они включены в JAR (fat jar/shadow jar).

## Рекомендации
- Для сложных модулей используйте отдельный пакет (namespace).
- Для доступа к API ядра используйте только публичные интерфейсы из пакета `dev.loadless.api`.
- Не используйте прямой доступ к внутренним классам ядра — это нарушает модульность.

---

Для Lua-модулей и расширенной документации — см. отдельный раздел.
