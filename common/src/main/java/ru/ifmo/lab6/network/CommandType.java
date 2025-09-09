package ru.ifmo.lab6.network;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Перечисление всех возможных типов команд с их описаниями.
 * Передается в объекте Request для идентификации команды на сервере.
 */
public enum CommandType implements Serializable {
    // Команды без аргументов
    HELP("help", "вывести справку по доступным командам"),
    INFO("info", "вывести информацию о коллекции"),
    SHOW("show", "вывести все элементы коллекции"),
    CLEAR("clear", "очистить коллекцию"),
    AVERAGE_OF_HEIGHT("average_of_height", "вывести среднее значение поля height"),

    // Команды с аргументами
    ADD("add {element}", "добавить новый элемент в коллекцию"),
    UPDATE("update id {element}", "обновить значение элемента коллекции"),
    REMOVE_BY_ID("remove_by_id id", "удалить элемент из коллекции по его id"),
    ADD_IF_MIN("add_if_min {element}", "добавить новый элемент, если его значение меньше минимального"),
    REMOVE_GREATER("remove_greater {element}", "удалить из коллекции все элементы, большие, чем заданный"),
    REMOVE_LOWER("remove_lower {element}", "удалить из коллекции все элементы, меньшие, чем заданный"),
    COUNT_BY_HAIR_COLOR("count_by_hair_color [hairColor]", "вывести количество элементов с заданным цветом волос"),
    FILTER_LESS_THAN_HAIR_COLOR("filter_less_than_hair_color [hairColor]", "вывести элементы, цвет волос которых меньше заданного"),

    // Команды, выполняемые только на клиенте
    EXIT("exit", "завершить работу клиента (без сохранения)"),
    EXECUTE_SCRIPT("execute_script file_name", "исполнить скрипт из файла");


    private final String signature;
    private final String description;

    CommandType(String signature, String description) {
        this.signature = signature;
        this.description = description;
    }

    public String getSignature() {
        return signature;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Статический метод для генерации полной справки по всем командам.
     * @return Отформатированная строка со списком всех команд и их описаний.
     */
    public static String getHelp() {
        return Arrays.stream(CommandType.values())
                .map(command -> String.format("  %-40s - %s", command.getSignature(), command.getDescription()))
                .collect(Collectors.joining("\n", "Доступные команды:\n", ""));
    }
}