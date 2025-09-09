package ru.ifmo.lab6.server;

import ru.ifmo.lab6.server.managers.CollectionManager;
import ru.ifmo.lab6.server.managers.XmlFileManager;
import ru.ifmo.lab6.server.util.LoggerSetup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс серверного приложения.
 * Реализует строго однопоточный цикл событий (event loop) для обработки
 * как сетевых запросов, так и команд из консоли сервера.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LoggerSetup.setup("server.log");

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
            // Инициализация всех компонентов
            final XmlFileManager xmlFileManager = new XmlFileManager(filePath);
            final CollectionManager collectionManager = new CollectionManager(xmlFileManager.load());
            final NetworkManager networkManager = new NetworkManager(port, new CommandExecutor(collectionManager));

            networkManager.setup();

            // Shutdown Hook оставляем как страховку на случай аварийного завершения
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutdown Hook: Сохранение коллекции...");
                xmlFileManager.save(collectionManager.getCollection());
                LOGGER.info("Shutdown Hook: Сохранение завершено.");
            }));

            // Используем BufferedReader для неблокирующей проверки консоли
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Сервер запущен. Введите 'save' для сохранения или 'exit' для завершения.");

            boolean running = true;
            // Главный однопоточный цикл событий
            while (running) {
                // 1. Проверяем консольный ввод (неблокирующе)
                if (consoleReader.ready()) {
                    String serverCommand = consoleReader.readLine().trim();
                    switch (serverCommand.toLowerCase()) {
                        case "save":
                            System.out.println("Выполняется сохранение коллекции...");
                            xmlFileManager.save(collectionManager.getCollection());
                            System.out.println("Коллекция успешно сохранена.");
                            break;
                        case "exit":
                            System.out.println("Завершение работы сервера...");
                            xmlFileManager.save(collectionManager.getCollection());
                            running = false; // Устанавливаем флаг для выхода из цикла
                            break;
                        default:
                            System.out.println("Неизвестная серверная команда. Доступные: 'save', 'exit'.");
                            break;
                    }
                }

                // 2. Проверяем сетевые запросы от клиентов (неблокирующе)
                networkManager.checkConnections();

                // 3. Небольшая пауза, чтобы цикл не потреблял 100% CPU
                Thread.sleep(100);
            }

            // Корректно закрываем ресурсы перед выходом
            networkManager.close();
            System.out.println("Сервер остановлен.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Произошла критическая ошибка в главном цикле сервера", e);
        }
    }
}