package ru.ifmo.lab6.client;

import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import ru.ifmo.lab6.client.managers.CommandFactory;
import ru.ifmo.lab6.client.managers.UserInputHandler;
import ru.ifmo.lab6.client.util.ConsoleInputProvider;
import ru.ifmo.lab6.client.util.ScriptInputProvider;
import ru.ifmo.lab6.network.CommandType;
import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;
import ru.ifmo.lab6.client.network.NetworkManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    }

    /**
     * Инициализирует клиент. Пытается подключиться к серверу для получения актуальных
     * команд. В случае неудачи настраивает клиент с базовым набором команд.
     * Этот метод гарантирует, что клиент будет готов к работе в любом случае.
     */
    public void initialize() {
        while (true) {
            System.out.println("Попытка подключения к серверу для настройки автодополнения...");
            try {
                Request helpRequest = new Request(CommandType.HELP);
                Response response = networkManager.sendAndReceive(helpRequest);

                if (response != null && response.getStatus() == Response.Status.SUCCESS && response.getData() instanceof Collection) {
                    System.out.println("Сервер доступен. Настройка автодополнения...");

                    @SuppressWarnings("unchecked")
                    Collection<String> commands = (Collection<String>) response.getData();
                    Completer completer = new StringsCompleter(commands);

                    this.lineReader = LineReaderBuilder.builder()
                            .terminal(terminal)
                            .completer(completer)
                            .build();

                    UserInputHandler consoleInputHandler = new UserInputHandler(new ConsoleInputProvider(lineReader));
                    this.commandFactory = new CommandFactory(consoleInputHandler);

                    System.out.println("\n" + response.getMessage());
                    return;

                } else {
                    throw new IOException("Сервер вернул некорректный ответ.");
                }

            } catch (IOException e) {
                System.err.println("Ошибка подключения: " + e.getMessage());
                try {
                    LineReader tempReader = LineReaderBuilder.builder().terminal(terminal).build();
                    String answer = tempReader.readLine("Повторить попытку? (yes/no): ").trim().toLowerCase();
                    if (!"yes".equals(answer) && !"y".equals(answer)) {
                        break;
                    }
                } catch (UserInterruptException | EndOfFileException ex) {
                    break;
                }
            }
        }
        setupDefaultComponents();
    }

    /**
     * Настраивает компоненты клиента с базовым (неполным) функционалом,
     * когда сервер недоступен при запуске.
     */
    private void setupDefaultComponents() {
        System.out.println("\nВНИМАНИЕ: Не удалось получить список команд с сервера.");
        System.out.println("Клиент запускается в автономном режиме. Автодополнение будет основано на стандартном списке команд.");

        Completer defaultCompleter = new StringsCompleter(
                Arrays.stream(CommandType.values())
                        .map(CommandType::getSignature)
                        .map(s -> s.split(" ")[0])
                        .collect(Collectors.toList())
        );

        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(defaultCompleter)
                .build();

        UserInputHandler consoleInputHandler = new UserInputHandler(new ConsoleInputProvider(lineReader));
        this.commandFactory = new CommandFactory(consoleInputHandler);
    }


    public void run() {
        if (commandFactory == null) {
            System.err.println("Критическая ошибка: клиент не был инициализирован. Завершение работы.");
            return;
        }

        System.out.println("\nВведите команду. Используйте Tab для автодополнения.");

        while (running) {
            try {
                String line = lineReader.readLine("> ");
                if (line == null) break;

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
                        Request request = commandFactory.createRequest(trimmedLine);
                        if (request != null) {
                            processRequest(request);
                        }
                        break;
                }
            } catch (UserInterruptException e) {
                System.out.println("\nПолучен сигнал прерывания (Ctrl+C). Для выхода введите 'exit'.");
            } catch (NoSuchElementException e) {
                System.err.println("\nПоток ввода был неожиданно прерван. Для выхода введите 'exit'.");
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
            UserInputHandler scriptInputHandler = new UserInputHandler(new ScriptInputProvider(fileScanner));
            CommandFactory scriptCommandFactory = new CommandFactory(scriptInputHandler);

            while (true) {
                try {
                    String line = scriptInputHandler.readLine("");
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.trim().split("\\s+", 2);
                    String commandName = parts[0].toLowerCase();
                    String arg = parts.length > 1 ? parts[1] : null;

                    if (commandName.equals("execute_script")) {
                        if (arg != null) {
                            executeScript(arg);
                        } else {
                            System.err.println("Необходимо указать имя файла для execute_script.");
                        }
                    } else {
                        Request request = scriptCommandFactory.createRequest(line);
                        if (request != null) {
                            this.processRequest(request);
                        }
                    }
                } catch (NoSuchElementException e) {
                    break;
                }
            }
            System.out.println("--- Завершение скрипта: " + fileName + " ---");
        } catch (FileNotFoundException e) {
            System.err.println("Ошибка: файл скрипта не найден: " + fileName);
        } catch (Exception e) {
            System.err.println("Критическая ошибка при выполнении скрипта '" + fileName + "': " + e.getMessage());
            e.printStackTrace();
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