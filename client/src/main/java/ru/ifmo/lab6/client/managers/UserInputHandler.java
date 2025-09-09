package ru.ifmo.lab6.client.managers;

import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import ru.ifmo.lab6.model.*;

import java.util.NoSuchElementException;

/**
 * Обрабатывает ввод данных пользователем для создания объектов.
 * В этой версии он работает ИСКЛЮЧИТЕЛЬНО с LineReader для консоли
 * или с переданным Scanner для скриптов, чтобы избежать конфликтов.
 */
public class UserInputHandler {
    private final LineReader lineReader; // Для интерактивного ввода
    private final java.util.Scanner scriptScanner; // Для ввода из файла
    private final boolean isScript;

    // Конструктор для работы с консолью
    public UserInputHandler(LineReader lineReader) {
        this.lineReader = lineReader;
        this.scriptScanner = null;
        this.isScript = false;
    }

    // Конструктор для работы со скриптом
    public UserInputHandler(java.util.Scanner scriptScanner) {
        this.lineReader = null;
        this.scriptScanner = scriptScanner;
        this.isScript = true;
    }

    /**
     * Читает одну строку ввода, используя правильный источник (консоль или файл).
     * @param prompt Приглашение к вводу для консоли.
     * @return Прочитанная строка.
     */
    private String readLine(String prompt) {
        try {
            if (isScript) {
                String line = scriptScanner.nextLine().trim();
                System.out.println(line); // Эхо-вывод для скрипта
                return line;
            } else {
                return lineReader.readLine(prompt + " ");
            }
        } catch (UserInterruptException e) {
            // Если пользователь нажал Ctrl+C, выбрасываем исключение наверх
            throw new NoSuchElementException("Ввод был прерван пользователем.");
        } catch (org.jline.reader.EndOfFileException e) {
            // Если пользователь нажал Ctrl+D
            throw new NoSuchElementException("Достигнут конец потока ввода.");
        }
    }

    /**
     * Запрашивает у пользователя все данные для создания нового объекта Person.
     * @return Готовый объект Person.
     * @throws NoSuchElementException если ввод прерван.
     */
    public Person requestPersonData() throws NoSuchElementException {
        System.out.println("Ввод данных для нового человека:");
        String name = requestString("Введите имя (не может быть пустым):", false);
        Coordinates coordinates = requestCoordinatesData();
        long height = requestLong("Введите рост (целое число > 0):", 1L, null);
        Color eyeColor = requestEnum(Color.class, "цвет глаз", true);
        Color hairColor = requestEnum(Color.class, "цвет волос", true);
        Country nationality = requestEnum(Country.class, "национальность", true);
        Location location = requestLocationData();
        return new Person(name, coordinates, height, eyeColor, hairColor, nationality, location);
    }

    private Coordinates requestCoordinatesData() {
        System.out.println("Ввод координат:");
        Double x = requestDouble("  Введите координату X (дробное число, max: 348):", null, 348.0);
        float y = requestFloat("  Введите координату Y (дробное число):", null, null);
        return new Coordinates(x, y);
    }

    private Location requestLocationData() {
        System.out.println("Ввод местоположения:");
        Float x = requestFloat("  Введите координату X местоположения (дробное число):", null, null);
        double y = requestDouble("  Введите координату Y местоположения (дробное число):", null, null);
        Double z = requestDouble("  Введите координату Z местоположения (дробное число):", null, null);
        String name = requestString("  Введите название местоположения (до 400 символов, можно оставить пустым):", true, 400);
        return new Location(x, y, z, name);
    }

    public String requestString(String prompt, boolean nullable, Integer maxLength) {
        while (true) {
            String input = readLine(prompt).trim();
            if (input.isEmpty()) {
                if (nullable) return null;
                System.err.println("Ошибка: Это поле не может быть пустым.");
            } else if (maxLength != null && input.length() > maxLength) {
                System.err.println("Ошибка: Длина строки не должна превышать " + maxLength + " символов.");
            } else {
                return input;
            }
        }
    }

    public String requestString(String prompt, boolean nullable) {
        return requestString(prompt, nullable, null);
    }

    public <T extends Enum<T>> T requestEnum(Class<T> enumClass, String fieldName, boolean nullable) {
        System.out.println("Выберите " + fieldName + ":");
        String values = enumClass == Color.class ? Color.listValues() : Country.listValues();
        System.out.println("Доступные значения: " + values);
        while (true) {
            try {
                String input = requestString("Введите название (или оставьте пустым, если разрешено null):", true);
                if (input == null || input.isEmpty()) {
                    if (nullable) return null;
                    else {
                        System.err.println("Ошибка: Это поле не может быть пустым.");
                        continue;
                    }
                }
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Ошибка: Некорректное значение. Пожалуйста, выберите из списка.");
            }
        }
    }

    private <T extends Number & Comparable<T>> T requestNumber(String prompt, T min, T max, String typeName, java.util.function.Function<String, T> parser) {
        while (true) {
            try {
                String inputStr = requestString(prompt, false);
                T value = parser.apply(inputStr.replace(',', '.'));
                if (min != null && value.compareTo(min) < 0) {
                    System.err.println("Ошибка: Значение должно быть не меньше " + min + ".");
                } else if (max != null && value.compareTo(max) > 0) {
                    System.err.println("Ошибка: Значение должно быть не больше " + max + ".");
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.err.println("Ошибка: Некорректный ввод. Пожалуйста, введите " + typeName + ".");
            }
        }
    }

    public float requestFloat(String prompt, Float min, Float max) {
        return requestNumber(prompt, min, max, "дробное число", Float::parseFloat);
    }
    public Double requestDouble(String prompt, Double min, Double max) {
        return requestNumber(prompt, min, max, "дробное число", Double::parseDouble);
    }
    public long requestLong(String prompt, Long min, Long max) {
        return requestNumber(prompt, min, max, "целое число", Long::parseLong);
    }
}