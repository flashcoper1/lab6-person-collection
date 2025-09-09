package ru.ifmo.lab6.client.managers;

import ru.ifmo.lab6.client.Client;
import ru.ifmo.lab6.command.Command;
import ru.ifmo.lab6.model.Color;
import ru.ifmo.lab6.model.Person;
import ru.ifmo.lab6.network.CommandType;
import ru.ifmo.lab6.network.Request;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Создает объекты Request на основе ввода пользователя.
 * Отвечает за парсинг команд и их аргументов.
 */
public class CommandFactory {
    private final UserInputHandler consoleInputHandler;
    private static final Set<String> scriptHistory = new HashSet<>();

    public CommandFactory(UserInputHandler consoleInputHandler) {
        this.consoleInputHandler = consoleInputHandler;
    }

    /**
     * Создает запрос на основе одной строки ввода.
     * @param line Введенная пользователем строка.
     * @return Объект Request или null, если команда не требует отправки (например, exit).
     */
    public Request createRequest(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : null;

        try {
            switch (commandName) {
                // Команды без аргументов
                case "help": return new Request(CommandType.HELP);
                case "info": return new Request(CommandType.INFO);
                case "show": return new Request(CommandType.SHOW);
                case "clear": return new Request(CommandType.CLEAR);
                case "average_of_height": return new Request(CommandType.AVERAGE_OF_HEIGHT);

                // Команды с простыми аргументами
                case "remove_by_id":
                    if (arg == null) throw new IllegalArgumentException("Необходимо указать ID.");
                    long id = Long.parseLong(arg);
                    return new Request(CommandType.REMOVE_BY_ID, new Command.RemoveById(id));

                // Команды со сложными аргументами (объектами)
                case "add":
                    return new Request(CommandType.ADD, new Command.Add(consoleInputHandler.requestPersonData()));
                case "add_if_min":
                    return new Request(CommandType.ADD_IF_MIN, new Command.AddIfMin(consoleInputHandler.requestPersonData()));
                case "update":
                    if (arg == null) throw new IllegalArgumentException("Необходимо указать ID.");
                    long updateId = Long.parseLong(arg);
                    System.out.println("Ввод новых данных для элемента с ID " + updateId);
                    return new Request(CommandType.UPDATE, new Command.Update(updateId, consoleInputHandler.requestPersonData()));

                // Команды с "полусложными" аргументами
                case "remove_greater":
                case "remove_lower":
                    System.out.println("Введите данные эталонного элемента для сравнения:");
                    Person thresholdPerson = consoleInputHandler.requestPersonData();
                    if (commandName.equals("remove_greater")) {
                        return new Request(CommandType.REMOVE_GREATER, new Command.RemoveGreater(thresholdPerson));
                    } else {
                        return new Request(CommandType.REMOVE_LOWER, new Command.RemoveLower(thresholdPerson));
                    }

                case "count_by_hair_color":
                case "filter_less_than_hair_color":
                    Color color = consoleInputHandler.requestEnum(Color.class, "цвет волос", true);
                    if (commandName.equals("count_by_hair_color")) {
                        return new Request(CommandType.COUNT_BY_HAIR_COLOR, new Command.CountByHairColor(color));
                    } else {
                        return new Request(CommandType.FILTER_LESS_THAN_HAIR_COLOR, new Command.FilterLessThanHairColor(color));
                    }

                    // Локальные команды клиента
                case "exit":
                    return null; // Сигнал для завершения работы клиента

                case "execute_script":
                    if (arg == null) throw new IllegalArgumentException("Необходимо указать имя файла.");
                    return new Request(CommandType.EXECUTE_SCRIPT, new Command.ExecuteScript(arg));

                default:
                    System.err.println("Неизвестная команда: " + commandName);
                    return new Request(CommandType.HELP); // Показываем помощь по умолчанию
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: Аргумент должен быть числом.");
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Произошла ошибка при создании команды: " + e.getMessage());
        }
        return null; // В случае ошибки не отправляем запрос
    }

    /**
     * Выполняет скрипт из файла.
     * @param fileName Имя файла.
     * @param client Ссылка на клиент для рекурсивного вызова.
     */
    public static void executeScript(String fileName, Client client) {
        if (scriptHistory.contains(fileName)) {
            System.err.println("Ошибка: Рекурсивный вызов скрипта! Файл '" + fileName + "' уже исполняется.");
            return;
        }
        scriptHistory.add(fileName);

        try (Scanner fileScanner = new Scanner(new File(fileName))) {
            System.out.println("--- Исполнение скрипта: " + fileName + " ---");
            UserInputHandler scriptInputHandler = new UserInputHandler(fileScanner);
            CommandFactory scriptCommandFactory = new CommandFactory(scriptInputHandler);

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                if (line.trim().isEmpty()) continue;
                System.out.println("> " + line); // Эхо-вывод команды

                if(line.trim().startsWith("execute_script")){
                    // Обработка вложенных скриптов
                    String[] parts = line.trim().split("\\s+", 2);
                    if(parts.length > 1){
                        executeScript(parts[1], client);
                    } else {
                        System.err.println("Необходимо указать имя файла для execute_script.");
                    }
                } else {
                    Request request = scriptCommandFactory.createRequest(line);
                    if (request != null) {
                        client.processRequest(request);
                    }
                }
            }
            System.out.println("--- Завершение скрипта: " + fileName + " ---");
        } catch (FileNotFoundException e) {
            System.err.println("Ошибка: файл скрипта не найден: " + fileName);
        } catch (Exception e) {
            System.err.println("Критическая ошибка при выполнении скрипта: " + e.getMessage());
        } finally {
            scriptHistory.remove(fileName);
        }
    }
}