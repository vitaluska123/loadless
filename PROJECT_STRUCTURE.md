# Структура проекта Loadless

```
loadless/
├── build.gradle         # build-скрипт Gradle
├── settings.gradle      # имя проекта
├── README.MD            # описание проекта
└── src/
    └── main/
        └── java/
            └── dev/
                └── loadless/
                    └── Main.java  # точка входа
```

- Для сборки fat JAR используем Gradle + Shadow plugin.
- Поддержка Java 17-24 (минимальная версия — 17).
- Для запуска: `java -jar loadless-1.0.0.jar`
- Совместимость с Minecraft 1.7.10 — 1.21.7 будет обеспечиваться на уровне протокола (реализуется в коде).
