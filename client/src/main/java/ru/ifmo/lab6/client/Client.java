package ru.ifmo.lab6.client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import ru.ifmo.lab6.client.managers.CommandFactory;
import ru.ifmo.lab6.client.managers.UserInputHandler;
import ru.ifmo.lab6.client.network.NetworkManager;
import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Главный класс клиента. Управляет циклом работы,
 * читает команды, отправляет их серверу и выводит результат.
 */
public class Client {
    private final NetworkManager networkManager;
    private final CommandFactory commandFactory;
    private final LineReader lineReader;
    private final Terminal terminal;
    private boolean running = true;

    public Client(String host, int port) throws IOException {
        this.networkManager = new NetworkManager(host, port);
        this.terminal = TerminalBuilder.builder().system(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        UserInputHandler consoleInputHandler = new UserInputHandler(lineReader);
        this.commandFactory = new CommandFactory(consoleInputHandler);
    }

    public void run() {
        System.out.println("Клиентское приложение для управления коллекцией Person.");
        System.out.println("Введите 'help' для получения списка команд.");

        while (running) {
            try {
                String line = lineReader.readLine("> ");
                if (line == null) { // Ctrl+D
                    break;
                }
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String[] parts = trimmedLine.split("\\s+", 2);
                String commandName = parts[0].toLowerCase();

                // --- НОВАЯ ЛОГИКА ОБРАБОТКИ ---
                if (commandName.equals("exit")) {
                    stop();
                    continue;
                }

                if (commandName.equals("execute_script")) {
                    if (parts.length > 1) {
                        CommandFactory.executeScript(parts[1], this);
                    } else {
                        System.err.println("Необходимо указать имя файла скрипта.");
                    }
                    continue; // Переходим к следующей итерации цикла
                }
                // -----------------------------

                Request request = commandFactory.createRequest(trimmedLine);
                if (request != null) {
                    processRequest(request);
                }

            } catch (UserInterruptException e) { // Ctrl+C
                System.out.println("\nДля выхода введите 'exit'.");
            } catch (NoSuchElementException e) { // Ctrl+D во время ввода
                break;
            }
        }
        stop();
    }

    public void processRequest(Request request) {
        try {
            Response response = networkManager.sendAndReceive(request);
            if (response != null) {
                handleResponse(response);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при обмене данными с сервером: " + e.getMessage());
        }
    }

    private void handleResponse(Response response) {
        if (response.getStatus() == Response.Status.ERROR) {
            System.err.println("Ошибка от сервера: " + response.getMessage());
        } else {
            if (response.getMessage() != null && !response.getMessage().isEmpty()) {
                System.out.println(response.getMessage());
            }
            if (response.getData() != null) {
                if (response.getData() instanceof Collection) {
                    Collection<?> collection = (Collection<?>) response.getData();
                    if (collection.isEmpty()){
                        System.out.println("Коллекция пуста.");
                    } else {
                        collection.forEach(item -> {
                            System.out.println(item.toString());
                            System.out.println("---");
                        });
                    }
                } else {
                    System.out.println(response.getData().toString());
                }
            }
        }
    }

    public void stop() {
        if (!running) return;
        this.running = false;
        System.out.println("Завершение работы клиента...");
        try {
            terminal.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии терминала: " + e.getMessage());
        }
        networkManager.close();
    }
}