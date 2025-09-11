package ru.ifmo.lab6.client;

import java.io.IOException;

/**
 * Точка входа в клиентское приложение.
 * Парсит аргументы командной строки и запускает клиент.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Использование: java -jar client.jar <host> <port>");
            return;
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: порт должен быть целым числом от 1 до 65535.");
            return;
        }

        try {
            Client client = new Client(host, port);
            client.initialize();
            client.run();
        } catch (IOException e) {
            System.err.println("Критическая ошибка при создании клиента: " + e.getMessage());
        }
    }
}