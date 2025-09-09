package ru.ifmo.lab6.server;

import ru.ifmo.lab6.server.managers.CollectionManager;
import ru.ifmo.lab6.server.managers.XmlFileManager;
import ru.ifmo.lab6.server.util.LoggerSetup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс серверного приложения.
 * Использует единый Selector для обработки как сетевых запросов, так и команд из консоли сервера,
 * реализуя полностью событийно-ориентированную однопоточную модель.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Настройка логгера
        LoggerSetup.setup("server.log");

        // 2. Валидация аргументов командной строки и переменной окружения
        if (args.length != 1) {
            System.err.println("Использование: java -jar server.jar <port>");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: порт должен быть целым числом от 1 до 65535.");
            return;
        }
        String filePath = System.getenv("PERSON_COLLECTION_FILE");
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Ошибка: Переменная окружения PERSON_COLLECTION_FILE не установлена.");
            return;
        }

        try {
            // 3. Инициализация основных компонентов сервера
            final XmlFileManager xmlFileManager = new XmlFileManager(filePath);
            final CollectionManager collectionManager = new CollectionManager(xmlFileManager.load());
            final NetworkManager networkManager = new NetworkManager(port, new CommandExecutor(collectionManager));

            networkManager.setup();

            // Используем AtomicBoolean для потокобезопасного управления состоянием работы сервера
            final AtomicBoolean running = new AtomicBoolean(true);

            // 4. Определяем логику обработки команд, поступающих с консоли сервера
            Consumer<String> consoleCommandHandler = (command) -> {
                switch (command.toLowerCase().trim()) {
                    case "save":
                        System.out.println("Выполняется сохранение коллекции...");
                        xmlFileManager.save(collectionManager.getCollection());
                        System.out.println("Коллекция успешно сохранена.");
                        break;
                    case "exit":
                        System.out.println("Завершение работы сервера...");
                        running.set(false); // Устанавливаем флаг для выхода из главного цикла
                        // Прерываем selector.select(), чтобы цикл завершился немедленно
                        networkManager.getSelector().wakeup();
                        break;
                    default:
                        System.out.println("Неизвестная серверная команда. Доступные: 'save', 'exit'.");
                        break;
                }
            };

            // 5. Создаем "мост" (Pipe) между блокирующим System.in и неблокирующим Selector'ом
            Pipe consolePipe = Pipe.open();
            networkManager.registerConsoleChannel(consolePipe.source(), consoleCommandHandler);

            // 6. Запускаем отдельный фоновый поток для чтения из блокирующего System.in
            Thread consoleInputThread = new Thread(() -> {
                try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
                    while (!Thread.currentThread().isInterrupted() && running.get()) {
                        String line = consoleReader.readLine(); // Этот вызов блокирует только этот поток
                        if (line == null) break; // EOF (Ctrl+D)
                        // Записываем прочитанную строку в Pipe. Данные "телепортируются" в главный поток.
                        consolePipe.sink().write(ByteBuffer.wrap(line.getBytes()));
                    }
                } catch (IOException e) {
                    if (running.get()) { // Логируем ошибку только если сервер не был остановлен намеренно
                        LOGGER.log(Level.SEVERE, "Ошибка в потоке чтения консоли", e);
                    }
                }
            });
            consoleInputThread.setDaemon(true); // Поток не будет мешать завершению программы
            consoleInputThread.start();

            // 7. Регистрируем Shutdown Hook для корректного сохранения при аварийном завершении (например, по Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutdown Hook: Сохранение коллекции...");
                xmlFileManager.save(collectionManager.getCollection());
                LOGGER.info("Shutdown Hook: Сохранение завершено.");
            }));

            System.out.println("Сервер запущен. Введите 'save' для сохранения или 'exit' для завершения.");

            // 8. Главный однопоточный цикл событий. Больше нет опросов и Thread.sleep()
            while (running.get()) {
                // Блокируется до тех пор, пока не произойдет какое-либо событие (сеть или консоль)
                networkManager.processEvents();
            }

            // 9. Корректное завершение работы
            System.out.println("Начинается процедура остановки сервера...");
            xmlFileManager.save(collectionManager.getCollection()); // Финальное сохранение
            networkManager.close();
            consolePipe.sink().close();
            consolePipe.source().close();
            System.out.println("Сервер остановлен.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Произошла критическая ошибка в главном цикле сервера", e);
        }
    }
}