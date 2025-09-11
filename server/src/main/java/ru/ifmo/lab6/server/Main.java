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

    private final XmlFileManager xmlFileManager;
    private final CollectionManager collectionManager;
    private final NetworkManager networkManager;
    private final Pipe consolePipe;

    public Main(int port, String filePath) throws IOException {
        this.xmlFileManager = new XmlFileManager(filePath);
        this.collectionManager = new CollectionManager(xmlFileManager.load());
        this.networkManager = new NetworkManager(port, new CommandExecutor(collectionManager));
        this.consolePipe = Pipe.open();
    }

    public void start() {
        try {
            networkManager.setup();
            networkManager.registerConsoleChannel(consolePipe.source(), this::handleConsoleCommand);

            Thread consoleInputThread = new Thread(this::readConsoleInput);
            consoleInputThread.setDaemon(true);
            consoleInputThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            System.out.println("Сервер запущен. Введите 'save' для сохранения или 'exit' для завершения.");

            while (running) {
                networkManager.processEvents();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Произошла критическая ошибка в главном цикле сервера", e);
        } finally {
            stop();
        }
    }

    private void handleConsoleCommand(String command) {
        switch (command.toLowerCase().trim()) {
            case "save":
                System.out.println("Выполняется сохранение коллекции...");
                xmlFileManager.save(collectionManager.getCollection());
                System.out.println("Коллекция успешно сохранена.");
                break;
            case "exit":
                System.out.println("Завершение работы сервера...");
                running = false;
                networkManager.getSelector().wakeup();
                break;
            default:
                System.out.println("Неизвестная серверная команда. Доступные: 'save', 'exit'.");
                break;
        }
    }

    private void readConsoleInput() {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running && !Thread.currentThread().isInterrupted()) {
                String line = consoleReader.readLine();
                if (line == null) break;
                consolePipe.sink().write(ByteBuffer.wrap(line.getBytes()));
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.log(Level.WARNING, "Ошибка в потоке чтения консоли", e);
            }
        }
    }

    private void stop() {
        System.out.println("Начинается процедура остановки сервера...");
        shutdown();
        try {
            networkManager.close();
            consolePipe.sink().close();
            consolePipe.source().close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Ошибка при закрытии ресурсов", e);
        }
        System.out.println("Сервер остановлен.");
    }

    private void shutdown() {
        xmlFileManager.save(collectionManager.getCollection());
        LOGGER.info("Коллекция сохранена.");
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
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Не удалось запустить сервер.", e);
        }
    }
}