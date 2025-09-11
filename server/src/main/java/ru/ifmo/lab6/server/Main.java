package ru.ifmo.lab6.server;

import ru.ifmo.lab6.server.managers.CollectionManager;
import ru.ifmo.lab6.server.managers.XmlFileManager;
import ru.ifmo.lab6.server.util.LoggerSetup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс серверного приложения.
 * Инкапсулирует логику запуска и остановки сервера.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private volatile boolean running = true;

    private final int port;
    private final String filePath;

    public Main(int port, String filePath) {
        this.port = port;
        this.filePath = filePath;
    }

    public void start() {
        XmlFileManager xmlFileManager = new XmlFileManager(filePath);
        CollectionManager collectionManager = new CollectionManager(xmlFileManager.load());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            xmlFileManager.save(collectionManager.getCollection());
            LOGGER.info("Коллекция сохранена при завершении работы.");
        }));


        try {
            final Pipe consolePipe = Pipe.open();

            try (NetworkManager networkManager = new NetworkManager(port, new CommandExecutor(collectionManager));
                 Pipe.SourceChannel consoleSource = consolePipe.source();
                 Pipe.SinkChannel consoleSink = consolePipe.sink()) {

                networkManager.setup();
                networkManager.registerConsoleChannel(consoleSource,
                        (command) -> handleConsoleCommand(command, collectionManager, xmlFileManager, networkManager));

                Thread consoleInputThread = new Thread(() -> readConsoleInput(consoleSink));
                consoleInputThread.setDaemon(true);
                consoleInputThread.start();

                LOGGER.info("Сервер запущен. Введите 'save' для сохранения или 'exit' для завершения.");

                while (running) {
                    networkManager.processEvents();
                }

            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Произошла критическая ошибка в главном цикле сервера", e);
        } finally {
            LOGGER.info("Сервер остановлен.");
        }
    }


    private void handleConsoleCommand(String command, CollectionManager cm, XmlFileManager fm, NetworkManager nm) {
        switch (command.toLowerCase().trim()) {
            case "save":
                LOGGER.info("Выполняется принудительное сохранение коллекции по команде с консоли...");
                fm.save(cm.getCollection());
                LOGGER.info("Коллекция успешно сохранена.");
                break;
            case "exit":
                LOGGER.info("Завершение работы сервера по команде exit...");
                running = false;
                if (nm != null && nm.getSelector() != null && nm.getSelector().isOpen()) {
                    nm.getSelector().wakeup();
                }
                break;
            default:
                LOGGER.warning("Неизвестная серверная команда: '" + command + "'. Доступные: 'save', 'exit'.");
                break;
        }
    }

    private void readConsoleInput(Pipe.SinkChannel consoleSink) {

        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running && !Thread.currentThread().isInterrupted()) {
                String line = consoleReader.readLine();
                if (line == null) break;
                if (!consoleSink.isOpen()) break; // Дополнительная проверка на случай, если канал уже закрыт
                consoleSink.write(ByteBuffer.wrap(line.getBytes()));
            }
        } catch (IOException e) {
            if (running && consoleSink.isOpen()) {
                LOGGER.log(Level.WARNING, "Ошибка в потоке чтения консоли", e);
            }
        }
    }


    public static void main(String[] args) {
        LoggerSetup.setup("server.log");

        if (args.length != 1) {
            System.err.println("Использование: java -jar server.jar <port>");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: порт должен быть целым числом.");
            return;
        }
        String filePath = System.getenv("PERSON_COLLECTION_FILE");
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Ошибка: Переменная окружения PERSON_COLLECTION_FILE не установлена.");
            return;
        }

        try {
            Main server = new Main(port, filePath);
            server.start();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Не удалось запустить сервер.", e);
        }
    }
}