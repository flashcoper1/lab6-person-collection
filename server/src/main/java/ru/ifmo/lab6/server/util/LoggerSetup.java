package ru.ifmo.lab6.server.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.ConsoleHandler;
/**
 * Утилитарный класс для централизованной настройки глобального логгера приложения.
 * Гарантирует, что все сообщения будут записываться в указанный файл в простом текстовом формате.
 */
public final class LoggerSetup {

    /**
     * Приватный конструктор, чтобы предотвратить создание экземпляров этого утилитарного класса.
     */
    private LoggerSetup() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Настраивает корневой логгер для записи сообщений в файл.
     * Если файл логов или родительские директории не существуют, они будут созданы.
     *
     * @param fileName Имя файла для логов (например, "server.log").
     */
    public static void setup(final String fileName) {
        try {
            // Получаем корневой логгер, чтобы перехватывать все сообщения в приложении.
            Logger rootLogger = Logger.getLogger("");

            // Удаляем все стандартные обработчики (например, ConsoleHandler),
            // чтобы избежать дублирования вывода в консоль.
            // Это гарантирует, что логи будут идти только в файл.
            for (var handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // Проверяем и создаем директории, если их нет
            Path logFilePath = Paths.get(fileName);
            if (logFilePath.getParent() != null) {
                Files.createDirectories(logFilePath.getParent());
            }

            // Создаем FileHandler. Второй аргумент 'true' означает, что файл будет дозаписываться, а не перезаписываться.
            FileHandler fileHandler = new FileHandler(fileName, true);

            // Используем SimpleFormatter для простого и читаемого формата логов:
            // [Дата и время] [Уровень]: [Сообщение]
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            // Добавляем наш файловый обработчик к корневому логгеру.
            rootLogger.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(consoleHandler);

            // Устанавливаем уровень логирования. INFO и выше (WARNING, SEVERE) будут записаны.
            rootLogger.setLevel(Level.INFO);

        } catch (IOException e) {
            // Если настройка логгера провалилась, это критическая ошибка конфигурации.
            // Выводим сообщение в System.err, так как сам логгер может быть неработоспособен.
            System.err.println("Критическая ошибка: не удалось настроить файловый логгер.");
            e.printStackTrace();
        }
    }
}