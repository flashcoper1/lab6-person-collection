package ru.ifmo.lab6.client.managers;

import ru.ifmo.lab6.client.Client;
import ru.ifmo.lab6.command.Command;
import ru.ifmo.lab6.model.Color;
import ru.ifmo.lab6.model.Person;
import ru.ifmo.lab6.network.CommandType;
import ru.ifmo.lab6.network.Request;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

/**
 * Создает объекты Request на основе ввода пользователя.
 * Теперь также содержит логику для локального выполнения команды execute_script.
 */
public class CommandFactory {
    private final UserInputHandler inputHandler;
    private static final Set<String> scriptHistory = new HashSet<>();

    public CommandFactory(UserInputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    /**
     * Создает запрос на основе одной строки ввода.
     * @param line Введенная пользователем строка.
     * @return Объект Request или null, если команда локальная (exit) или произошла ошибка.
     */
    public Request createRequest(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : null;

        try {
            switch (commandName) {
                // Команды, отправляемые на сервер
                case "help": return new Request(CommandType.HELP);
                case "info": return new Request(CommandType.INFO);
                case "show": return new Request(CommandType.SHOW);
                case "clear": return new Request(CommandType.CLEAR);
                case "average_of_height": return new Request(CommandType.AVERAGE_OF_HEIGHT);

                case "remove_by_id":
                    if (arg == null) throw new IllegalArgumentException("Необходимо указать ID.");
                    long id = Long.parseLong(arg);
                    return new Request(CommandType.REMOVE_BY_ID, new Command.RemoveById(id));

                case "add":
                    return new Request(CommandType.ADD, new Command.Add(inputHandler.requestPersonData()));
                case "add_if_min":
                    return new Request(CommandType.ADD_IF_MIN, new Command.AddIfMin(inputHandler.requestPersonData()));
                case "update":
                    if (arg == null) throw new IllegalArgumentException("Необходимо указать ID.");
                    long updateId = Long.parseLong(arg);
                    System.out.println("Ввод новых данных для элемента с ID " + updateId);
                    return new Request(CommandType.UPDATE, new Command.Update(updateId, inputHandler.requestPersonData()));

                case "remove_greater":
                case "remove_lower":
                    System.out.println("Введите данные эталонного элемента для сравнения:");
                    Person thresholdPerson = inputHandler.requestPersonData();
                    if (commandName.equals("remove_greater")) {
                        return new Request(CommandType.REMOVE_GREATER, new Command.RemoveGreater(thresholdPerson));
                    } else {
                        return new Request(CommandType.REMOVE_LOWER, new Command.RemoveLower(thresholdPerson));
                    }

                case "count_by_hair_color":
                case "filter_less_than_hair_color":
                    Color color = inputHandler.requestEnum(Color.class, "цвет волос", true);
                    if (commandName.equals("count_by_hair_color")) {
                        return new Request(CommandType.COUNT_BY_HAIR_COLOR, new Command.CountByHairColor(color));
                    } else {
                        return new Request(CommandType.FILTER_LESS_THAN_HAIR_COLOR, new Command.FilterLessThanHairColor(color));
                    }

                    // Локальные команды клиента
                case "exit":
                case "execute_script":
                    // Эти команды обрабатываются в Client.java, здесь просто возвращаем null
                    return null;

                default:
                    System.err.println("Неизвестная команда: " + commandName);
                    return new Request(CommandType.HELP); // Показываем помощь по умолчанию
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: Аргумент должен быть числом.");
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
        } catch (NoSuchElementException e) {
            System.err.println("\nВвод был прерван (Ctrl+D). Команда не будет выполнена.");
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка при создании команды: " + e.getMessage());
        }
        return null; // В случае ошибки не отправляем запрос
    }

    /**
     * Выполняет скрипт из файла.
     * @param fileName Имя файла.
     * @param client Ссылка на клиент для рекурсивного вызова.
     */
    public static void executeScript(String fileName, Client client) {
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
                System.out.println("> " + line); // Эхо-вывод команды

                String[] parts = line.trim().split("\\s+", 2);
                String commandName = parts[0].toLowerCase();
                String arg = parts.length > 1 ? parts[1] : null;

                if (commandName.equals("execute_script")) {
                    if(arg != null){
                        executeScript(arg, client);
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
            System.err.println("Критическая ошибка при выполнении скрипта '" + fileName + "': " + e.getMessage());
        } finally {
            scriptHistory.remove(resolvedPath);
        }
    }
}