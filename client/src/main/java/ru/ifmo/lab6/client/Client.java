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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

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

    private final Set<String> scriptHistory = new HashSet<>();

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
                if (line == null) break; // Обработка Ctrl+D

                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;

                String[] parts = trimmedLine.split("\\s+", 2);
                String commandName = parts[0].toLowerCase();
                String arg = parts.length > 1 ? parts[1] : null;

                switch (commandName) {
                    case "exit":
                        running = false;
                        break;

                    case "execute_script":
                        if (arg == null) {
                            System.err.println("Ошибка: необходимо указать имя файла скрипта.");
                        } else {
                            executeScript(arg);
                        }
                        break;

                    default:
                        // Для всех остальных команд - отправляем на сервер
                        Request request = commandFactory.createRequest(trimmedLine);
                        if (request != null) {
                            processRequest(request);
                        }
                        break;
                }

            } catch (UserInterruptException e) {
                // Обработка Ctrl+C
                System.out.println("\nПолучен сигнал прерывания (Ctrl+C). Завершение работы...");
                running = false;
            } catch (NoSuchElementException e) {
                // Если ввод был прерван в UserInputHandler
                break;
            }
        }
        stop();
    }


    private void executeScript(String fileName) {
        String resolvedPath;
        try {
            resolvedPath = new File(fileName).getCanonicalPath();
        } catch (IOException e) {
            System.err.println("Ошибка при разрешении пути файла: " + fileName);
            return;
        }

        if (scriptHistory.contains(resolvedPath)) {
            System.err.println("Ошибка: Рекурсивный вызов скрипта! Файл '" + resolvedPath + "' уже исполняется.");
            return;
        }
        scriptHistory.add(resolvedPath);

        try (Scanner fileScanner = new Scanner(new File(fileName))) {
            System.out.println("--- Исполнение скрипта: " + fileName + " ---");
            UserInputHandler scriptInputHandler = new UserInputHandler(fileScanner);
            CommandFactory scriptCommandFactory = new CommandFactory(scriptInputHandler);

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                if (line.trim().isEmpty()) continue;
                System.out.println("> " + line);

                String[] parts = line.trim().split("\\s+", 2);
                String commandName = parts[0].toLowerCase();
                String arg = parts.length > 1 ? parts[1] : null;

                if (commandName.equals("execute_script")) {
                    if(arg != null){
                        // Рекурсивный вызов этого же метода
                        executeScript(arg);
                    } else {
                        System.err.println("Необходимо указать имя файла для execute_script.");
                    }
                } else {
                    // Используем `this` для вызова метода этого же объекта
                    Request request = scriptCommandFactory.createRequest(line);
                    if (request != null) {
                        this.processRequest(request);
                    }
                }
            }
            System.out.println("--- Завершение скрипта: " + fileName + " ---");
        } catch (FileNotFoundException e) {
            System.err.println("Ошибка: файл скрипта не найден: " + fileName);
        } catch (Exception e) {
            System.err.println("Критическая ошибка при выполнении скрипта '" + fileName + "': " + e.getMessage());
        } finally {
            scriptHistory.remove(resolvedPath);
        }
    }

    public void processRequest(Request request) {
        try {
            Response response = networkManager.sendAndReceive(request);
            if (response != null) {
                handleResponse(response, request);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при обмене данными с сервером: " + e.getMessage());
        }
    }

    private void handleResponse(Response response,Request request) {
        if (response.getStatus() == Response.Status.ERROR) {
            System.err.println("Ошибка от сервера: " + response.getMessage());
        } else {
            if (response.getMessage() != null && !response.getMessage().isEmpty()) {
                System.out.println(response.getMessage());
            }
            if (response.getData() != null && request.getCommandType() != CommandType.HELP) {
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