package dev.loadless.api;

public interface ConsoleCommand {
    /**
     * @return имя команды (без /)
     */
    String getName();

    /**
     * @return краткое описание
     */
    String getDescription();

    /**
     * Выполнить команду
     * @param args аргументы (без имени команды)
     * @return строка для вывода в консоль (или null)
     */
    String execute(String[] args);
}
