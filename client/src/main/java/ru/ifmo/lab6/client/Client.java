package ru.ifmo.lab6.client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import ru.ifmo.lab6.client.managers.CommandFactory;
import ru.ifmo.lab6.client.managers.UserInputHandler;
import ru.ifmo.lab6.client.network.NetworkManager;
import ru.ifmo.lab6.command.Command; // Нужен импорт для ExecuteScript
import ru.ifmo.lab6.network.CommandType;
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

    /**
     * Конструктор клиента. Инициализирует все необходимые компоненты.
     * @param host Хост сервера.
     * @param port Порт сервера.
     * @throws IOException если не удалось создать терминал или сетевое подключение.
     */
    public Client(String host, int port) throws IOException {
        this.networkManager = new NetworkManager(host, port);

        // Инициализируем терминал и LineReader здесь
        this.terminal = TerminalBuilder.builder().system(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();

        // Инициализируем обработчик ввода и фабрику команд, используя LineReader
        UserInputHandler consoleInputHandler = new UserInputHandler(lineReader);
        // Инициализируем ПОЛЕ КЛАССА commandFactory
        this.commandFactory = new CommandFactory(consoleInputHandler);
    }

    /**
     * Запускает главный цикл приложения.
     */
    public void run() {
        System.out.println("Клиентское приложение для управления коллекцией Person.");
        System.out.println("Введите 'help' для получения списка команд.");

        while (running) {
            try {
                String line = lineReader.readLine("> ");
                if (line == null) { // Ctrl+D
                    break;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Используем ПОЛЕ КЛАССА commandFactory
                Request request = commandFactory.createRequest(line);

                if (request == null) {
                    // Если createRequest вернул null, это может быть команда exit или ошибка
                    String commandName = line.trim().split("\\s+")[0].toLowerCase();
                    if (commandName.equals("exit")) {
                        stop();
                    }
                    continue;
                }

                if (request.getCommandType() == CommandType.EXECUTE_SCRIPT) {
                    Command.ExecuteScript args = (Command.ExecuteScript) request.getArguments();
                    CommandFactory.executeScript(args.fileName, this);
                } else {
                    processRequest(request);
                }

            } catch (UserInterruptException e) { // Ctrl+C
                System.out.println("\nДля выхода введите 'exit'.");
            } catch (NoSuchElementException e) { // Ctrl+D во время ввода
                break;
            }
        }

        stop(); // Убедимся, что все ресурсы будут закрыты
    }

    /**
     * Отправляет запрос на сервер и обрабатывает ответ.
     * @param request Запрос для отправки.
     */
    public void processRequest(Request request) {
        try {
            Response response = networkManager.sendAndReceive(request);
            if (response != null) {
                handleResponse(response);
            }
        } catch (IOException | ClassNotFoundException e) {
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

    /**
     * Останавливает клиент и освобождает ресурсы.
     */
    public void stop() {
        if (!running) return; // Предотвращаем повторный вызов
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