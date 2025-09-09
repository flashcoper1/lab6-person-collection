package ru.ifmo.lab6.server;

import ru.ifmo.lab6.command.Command;
import ru.ifmo.lab6.model.Person;
import ru.ifmo.lab6.network.CommandType;
import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;
import ru.ifmo.lab6.server.managers.CollectionManager;

import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Класс, отвечающий за выполнение команд, полученных от клиента.
 * Принимает запрос, определяет тип команды и вызывает соответствующий метод у CollectionManager.
 */
public class CommandExecutor {
    private static final Logger LOGGER = Logger.getLogger(CommandExecutor.class.getName());
    private final CollectionManager collectionManager;

    public CommandExecutor(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    /**
     * Выполняет запрос и возвращает ответ.
     * @param request Запрос от клиента.
     * @return Ответ сервера.
     */
    public Response execute(Request request) {
        CommandType type = request.getCommandType();
        Object args = request.getArguments();
        LOGGER.info("Выполнение команды " + type);

        try {
            switch (type) {
                case HELP:
                    return new Response(Response.Status.SUCCESS, getHelpMessage());
                case INFO:
                    return new Response(Response.Status.SUCCESS, collectionManager.getInfo());
                case SHOW:
                    return new Response(Response.Status.SUCCESS, "Элементы коллекции:", collectionManager.getCollection());
                case CLEAR:
                    return new Response(Response.Status.SUCCESS, collectionManager.clear());
                case AVERAGE_OF_HEIGHT:
                    double avg = collectionManager.getAverageHeight();
                    String avgMessage = collectionManager.getCollection().isEmpty()
                            ? "Коллекция пуста."
                            : "Средний рост: " + String.format("%.2f", avg);
                    return new Response(Response.Status.SUCCESS, avgMessage);

                case ADD:
                    return new Response(Response.Status.SUCCESS, collectionManager.add(((Command.Add) args).person));
                case UPDATE:
                    Command.Update updateArgs = (Command.Update) args;
                    return new Response(Response.Status.SUCCESS, collectionManager.update(updateArgs.id, updateArgs.person));
                case REMOVE_BY_ID:
                    return new Response(Response.Status.SUCCESS, collectionManager.removeById(((Command.RemoveById) args).id));
                case ADD_IF_MIN:
                    return new Response(Response.Status.SUCCESS, collectionManager.addIfMin(((Command.AddIfMin) args).person));
                case REMOVE_GREATER:
                    return new Response(Response.Status.SUCCESS, collectionManager.removeGreater(((Command.RemoveGreater) args).person));
                case REMOVE_LOWER:
                    return new Response(Response.Status.SUCCESS, collectionManager.removeLower(((Command.RemoveLower) args).person));
                case COUNT_BY_HAIR_COLOR:
                    long count = collectionManager.countByHairColor(((Command.CountByHairColor) args).hairColor);
                    return new Response(Response.Status.SUCCESS, "Количество элементов: " + count);
                case FILTER_LESS_THAN_HAIR_COLOR:
                    TreeSet<Person> filtered = collectionManager.filterLessThanHairColor(((Command.FilterLessThanHairColor) args).hairColor);
                    return new Response(Response.Status.SUCCESS, "Отфильтрованные элементы:", filtered);

                default:
                    return new Response(Response.Status.ERROR, "Неизвестная или неподдерживаемая команда на сервере: " + type);
            }
        } catch (ClassCastException e) {
            return new Response(Response.Status.ERROR, "Некорректный тип аргумента для команды " + type);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при выполнении команды " + type + ": " + e.getMessage());
            return new Response(Response.Status.ERROR, "Внутренняя ошибка сервера при выполнении команды: " + e.getMessage());
        }
    }

    private String getHelpMessage() {
        return "Доступные команды:\n" +
                "  help : вывести справку по доступным командам\n" +
                "  info : вывести информацию о коллекции\n" +
                "  show : вывести все элементы коллекции\n" +
                "  add {element} : добавить новый элемент в коллекцию\n" +
                "  update id {element} : обновить значение элемента коллекции\n" +
                "  remove_by_id id : удалить элемент из коллекции по его id\n" +
                "  clear : очистить коллекцию\n" +
                "  exit : завершить программу (клиент)\n" +
                "  add_if_min {element} : добавить новый элемент, если его значение меньше минимального\n" +
                "  remove_greater {element} : удалить из коллекции все элементы, большие, чем заданный\n" +
                "  remove_lower {element} : удалить из коллекции все элементы, меньшие, чем заданный\n" +
                "  average_of_height : вывести среднее значение поля height\n" +
                "  count_by_hair_color [hairColor] : вывести количество элементов с заданным цветом волос\n" +
                "  filter_less_than_hair_color [hairColor] : вывести элементы, цвет волос которых меньше заданного\n" +
                "  execute_script file_name : исполнить скрипт из файла";
    }
}