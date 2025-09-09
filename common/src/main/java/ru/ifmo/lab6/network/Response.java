package ru.ifmo.lab6.network;

import java.io.Serializable;

/**
 * Объект-контейнер для ответа от сервера клиенту.
 * Содержит статус выполнения, сообщение и, возможно, какие-либо данные.
 * Реализует Serializable для передачи по сети.
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 102L;

    public enum Status {
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final String message;
    private final Serializable data;

    public Response(Status status, String message, Serializable data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Response(Status status, String message) {
        this(status, message, null);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Serializable getData() {
        return data;
    }
}