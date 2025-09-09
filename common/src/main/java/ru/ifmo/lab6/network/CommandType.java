package ru.ifmo.lab6.network;

import java.io.Serializable;

/**
 * Перечисление всех возможных типов команд.
 * Передается в объекте Request для идентификации команды на сервере.
 */
public enum CommandType implements Serializable {
    // Команды без аргументов
    HELP,
    INFO,
    SHOW,
    CLEAR,
    EXIT, // Exit только для клиента, серверу не отправляется
    AVERAGE_OF_HEIGHT,

    // Команды с аргументами
    ADD,
    UPDATE,
    REMOVE_BY_ID,
    ADD_IF_MIN,
    REMOVE_GREATER,
    REMOVE_LOWER,
    COUNT_BY_HAIR_COLOR,
    FILTER_LESS_THAN_HAIR_COLOR,
    EXECUTE_SCRIPT
}