package ru.ifmo.lab6.client;

import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import ru.ifmo.lab6.client.managers.CommandFactory;
import ru.ifmo.lab6.client.managers.UserInputHandler;
import ru.ifmo.lab6.network.CommandType;
import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;
import ru.ifmo.lab6.client.network.NetworkManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * Главный класс клиента. Управляет циклом работы,
 * реализует отказоустойчивую инициализацию с автодополнением.
 */
public class Client {
    private final NetworkManager networkManager;
    private final Terminal terminal;
    private CommandFactory commandFactory;
    private LineReader lineReader;
    private boolean running = true;

    public Client(String host, int port) throws IOException {
        this.networkManager = new NetworkManager(host, port);
        this.terminal = TerminalBuilder.builder().system(true).build();
        // Создаем временный LineReader для диалога с пользователем при ошибках подключения
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    /**
     * Инициализирует клиент: получает список команд с сервера для автодополнения.
     * @return true, если инициализация успешна, false - если пользователь отказался.
     */
    public boolean initialize() {
        while (true) {
            System.out.println("Попытка подключения к серверу для настройки автодополнения...");
            try {
                // Отправляем HELP, чтобы получить и справку, и список команд
                Request helpRequest = new Request(CommandType.HELP);
                Response response = networkManager.sendAndReceive(helpRequest);

                if (response != null && response.getStatus() == Response.Status.SUCCESS && response.getData() instanceof Collection) {
                    System.out.println("Сервер доступен. Настройка автодополнения...");

                    @SuppressWarnings("unchecked")
                    Collection<String> commands = (Collection<String>) response.getData();

                    Completer completer = new StringsCompleter(commands);

                    // Пересоздаем LineReader с настроенным автодополнением
                    this.lineReader = LineReaderBuilder.builder()
                            .terminal(terminal)
                            .completer(completer)
                            .build();

                    // Теперь создаем CommandFactory с правильным LineReader'ом
                    UserInputHandler consoleInputHandler = new UserInputHandler(lineReader);
                    this.commandFactory = new CommandFactory(consoleInputHandler);

                    // Выводим приветственное сообщение и справку
                    System.out.println("\n" + response.getMessage());
                    return true; // Успех

                } else {
                    throw new IOException("Сервер вернул некорректный ответ.");
                }

            } catch (IOException e) {
                System.err.println("Ошибка инициализации: " + e.getMessage());
                try {
                    String answer = lineReader.readLine("Повторить попытку? (yes/no): ").trim().toLowerCase();
                    if (!answer.equals("yes") && !answer.equals("y")) {
                        return false; // Пользователь отказался
                    }
                } catch (UserInterruptException | EndOfFileException ex) {
                    return false;
                }
            }
        }
    }

    public void run() {
        if (commandFactory == null) {
            System.err.println("Клиент не был инициализирован. Завершение работы.");
            return;
        }

        System.out.println("\nВведите команду. Используйте Tab для автодополнения.");

        while (running) {
            try {
                String line = lineReader.readLine("> ");
                if (line == null) break;

                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;

                Request request = commandFactory.createRequest(trimmedLine);
                if (request != null) {
                    processRequest(request);
                }

            } catch (UserInterruptException e) {
                System.out.println("\nДля выхода введите 'exit'.");
            } catch (NoSuchElementException e) {
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