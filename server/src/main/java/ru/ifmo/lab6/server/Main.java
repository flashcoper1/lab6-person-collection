package ru.ifmo.lab6.server;

import ru.ifmo.lab6.server.managers.CollectionManager;
import ru.ifmo.lab6.server.managers.XmlFileManager;
import ru.ifmo.lab6.server.util.LoggerSetup;

import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // --- ОТЛАДКА НАЧАЛАСЬ ---
        System.out.println("DEBUG: Точка входа main() достигнута.");

        // 1. Настройка логгера
        LoggerSetup.setup("server.log");
        System.out.println("DEBUG: Шаг 1: Настройка логгера завершена.");

        // 2. Валидация аргументов
        if (args.length != 1) {
            System.err.println("Использование: java -jar server.jar <port>");
            return;
        }
        System.out.println("DEBUG: Шаг 2: Валидация аргументов пройдена.");

        int port;
        try {
            port = Integer.parseInt(args[0]);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException("Порт вне допустимого диапазона.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: порт должен быть целым числом от 1 до 65535.");
            return;
        }
        System.out.println("DEBUG: Шаг 3: Порт " + port + " успешно распарсен.");

        // 3. Получение переменной окружения
        String filePath = System.getenv("PERSON_COLLECTION_FILE");
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Ошибка: Переменная окружения PERSON_COLLECTION_FILE не установлена.");
            return;
        }
        System.out.println("DEBUG: Шаг 4: Получен путь к файлу: " + filePath);

        // 4. Инициализация менеджеров
        System.out.println("DEBUG: Шаг 5: Начинаем инициализацию менеджеров...");
        XmlFileManager xmlFileManager = new XmlFileManager(filePath);
        System.out.println("DEBUG: XmlFileManager создан.");

        // --- ВОЗМОЖНОЕ МЕСТО ОШИБКИ ---
        CollectionManager collectionManager = new CollectionManager(xmlFileManager.load());
        System.out.println("DEBUG: CollectionManager создан.");

        CommandExecutor commandExecutor = new CommandExecutor(collectionManager);
        System.out.println("DEBUG: CommandExecutor создан.");

        NetworkManager networkManager = new NetworkManager(port, commandExecutor);
        System.out.println("DEBUG: NetworkManager создан.");
        System.out.println("DEBUG: Шаг 5: Инициализация менеджеров завершена.");

        // 5. Регистрация Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Получен сигнал завершения. Сохранение коллекции...");
            xmlFileManager.save(collectionManager.getCollection());
            networkManager.stop();
            LOGGER.info("Сервер завершает работу.");
        }));
        System.out.println("DEBUG: Шаг 6: Shutdown Hook зарегистрирован.");

        // 6. Запуск сервера
        System.out.println("DEBUG: Шаг 7: Запускаем NetworkManager...");
        networkManager.run();
    }
}