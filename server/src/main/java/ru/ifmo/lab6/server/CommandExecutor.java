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
 * Использует Pattern Matching для безопасной работы с аргументами команд (требуется Java 16+).
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
        final CommandType type = request.getCommandType();
        final Object args = request.getArguments();
        LOGGER.info("Выполнение команды " + type);

        try {
            switch (type) {
                // Команды без аргументов
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

                // Команды с аргументами, обработанные с помощью Pattern Matching
                case ADD:
                    if (args instanceof Command.Add addArgs) {
                        return new Response(Response.Status.SUCCESS, collectionManager.add(addArgs.person));
                    }
                    break; // break, чтобы попасть в общую обработку ошибки типа

                case UPDATE:
                    if (args instanceof Command.Update updateArgs) {
                        return new Response(Response.Status.SUCCESS, collectionManager.update(updateArgs.id, updateArgs.person));
                    }
                    break;

                case REMOVE_BY_ID:
                    if (args instanceof Command.RemoveById removeArgs) {
                        return new Response(Response.Status.SUCCESS, collectionManager.removeById(removeArgs.id));
                    }
                    break;

                case ADD_IF_MIN:
                    if (args instanceof Command.AddIfMin addIfMinArgs) {
                        return new Response(Response.Status.SUCCESS, collectionManager.addIfMin(addIfMinArgs.person));
                    }
                    break;

                case REMOVE_GREATER:
                    if (args instanceof Command.RemoveGreater removeGreaterArgs) {
                        return new Response(Response.Status.SUCCESS, collectionManager.removeGreater(removeGreaterArgs.person));
                    }
                    break;

                case REMOVE_LOWER:
                    if (args instanceof Command.RemoveLower removeLowerArgs) {
                        return new Response(Response.Status.SUCCESS, collectionManager.removeLower(removeLowerArgs.person));
                    }
                    break;

                case COUNT_BY_HAIR_COLOR:
                    if (args instanceof Command.CountByHairColor countArgs) {
                        long count = collectionManager.countByHairColor(countArgs.hairColor);
                        return new Response(Response.Status.SUCCESS, "Количество элементов: " + count);
                    }
                    break;

                case FILTER_LESS_THAN_HAIR_COLOR:
                    if (args instanceof Command.FilterLessThanHairColor filterArgs) {
                        TreeSet<Person> filtered = collectionManager.filterLessThanHairColor(filterArgs.hairColor);
                        return new Response(Response.Status.SUCCESS, "Отфильтрованные элементы:", filtered);
                    }
                    break;

                default:
                    return new Response(Response.Status.ERROR, "Неизвестная или неподдерживаемая команда на сервере: " + type);
            }

            // Если мы дошли сюда, значит, `instanceof` проверка для команды с аргументом провалилась
            return new Response(Response.Status.ERROR, "Некорректный тип аргумента для команды " + type);

        } catch (Exception e) {
            // Этот блок теперь будет ловить только непредвиденные ошибки, а не ClassCastException
            LOGGER.severe("Ошибка при выполнении команды " + type + ": " + e.getMessage());
            return new Response(Response.Status.ERROR, "Внутренняя ошибка сервера при выполнении команды: " + e.getMessage());
        }
    }

    /**
     * Генерирует и возвращает справку по командам.
     * @return Строка со справкой.
     */
    private String getHelpMessage() {
        return CommandType.getHelp();
    }
}