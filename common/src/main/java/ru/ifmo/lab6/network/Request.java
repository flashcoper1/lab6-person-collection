package ru.ifmo.lab6.network;

import java.io.Serializable;

/**
 * Объект-контейнер для запроса от клиента к серверу.
 * Содержит тип команды и её аргументы.
 * Реализует Serializable для передачи по сети.
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 101L;

    private final CommandType commandType;
    private final Serializable arguments;

    public Request(CommandType commandType, Serializable arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }

    public Request(CommandType commandType) {
        this(commandType, null);
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public Serializable getArguments() {
        return arguments;
    }
}