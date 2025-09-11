package ru.ifmo.lab6.client.managers;

import ru.ifmo.lab6.client.util.InputProvider;
import ru.ifmo.lab6.model.Color;
import ru.ifmo.lab6.model.Coordinates;
import ru.ifmo.lab6.model.Country;
import ru.ifmo.lab6.model.Location;
import ru.ifmo.lab6.model.Person;

import java.util.NoSuchElementException;

/**
 * Обрабатывает ввод данных пользователем для создания объектов.
 * Полностью абстрагирован от источника ввода через интерфейс InputProvider.
 * Может работать как с интерактивной консолью, так и с файлом скрипта без изменения кода.
 */
public class UserInputHandler {
    /**
     * Поставщик ввода, который инкапсулирует логику чтения из конкретного источника.
     */
    private final InputProvider provider;

    /**
     * Конструктор, который принимает любую реализацию InputProvider.
     * @param provider Поставщик ввода (например, из консоли или файла).
     */
    public UserInputHandler(InputProvider provider) {
        this.provider = provider;
    }

    /**
     * Читает одну строку ввода, используя переданный provider.
     * Этот метод является единственной точкой взаимодействия с источником данных.
     * @param prompt Приглашение к вводу (используется, если источник интерактивный).
     * @return Прочитанная строка.
     * @throws NoSuchElementException если ввод был прерван или источник исчерпан.
     */
    public String readLine(String prompt) throws NoSuchElementException {
        return provider.readLine(prompt);
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

    /**
     * Запрашивает данные для объекта Coordinates.
     * @return Новый объект Coordinates.
     */
    private Coordinates requestCoordinatesData() {
        System.out.println("Ввод координат:");
        Double x = requestDouble("  Введите координату X (дробное число, max: 348):", null, 348.0);
        float y = requestFloat("  Введите координату Y (дробное число):", null, null);
        return new Coordinates(x, y);
    }

    /**
     * Запрашивает данные для объекта Location.
     * @return Новый объект Location.
     */
    private Location requestLocationData() {
        System.out.println("Ввод местоположения:");
        Float x = requestFloat("  Введите координату X местоположения (дробное число):", null, null);
        double y = requestDouble("  Введите координату Y местоположения (дробное число):", null, null);
        Double z = requestDouble("  Введите координату Z местоположения (дробное число):", null, null);
        String name = requestString("  Введите название местоположения (до 400 символов, можно оставить пустым):", true, 400);
        return new Location(x, y, z, name);
    }

    /**
     * Запрашивает строковое значение с валидацией.
     * @param prompt Приглашение к вводу.
     * @param nullable Может ли значение быть null (пустая строка).
     * @param maxLength Максимальная допустимая длина строки.
     * @return Введенная строка или null.
     */
    public String requestString(String prompt, boolean nullable, Integer maxLength) {
        while (true) {
            String input = this.readLine(prompt).trim();
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

    /**
     * Перегруженный метод для запроса строки без ограничения длины.
     * @param prompt Приглашение к вводу.
     * @param nullable Может ли значение быть null.
     * @return Введенная строка или null.
     */
    public String requestString(String prompt, boolean nullable) {
        return requestString(prompt, nullable, null);
    }

    /**
     * Запрашивает значение из перечисления (enum).
     * @param enumClass Класс перечисления.
     * @param fieldName Имя поля для вывода в приглашении.
     * @param nullable Может ли значение быть null.
     * @return Выбранное значение enum или null.
     * @param <T> Тип перечисления.
     */
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

    /**
     * Универсальный метод для запроса числовых значений с валидацией.
     * @param prompt Приглашение к вводу.
     * @param min Минимальное допустимое значение.
     * @param max Максимальное допустимое значение.
     * @param typeName Название типа для сообщений об ошибках (например, "целое число").
     * @param parser Функция для парсинга строки в число.
     * @return Введенное и валидированное число.
     * @param <T> Тип числа (Long, Double, Float и т.д.).
     */
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

    /**
     * Запрашивает число типа float.
     */
    public float requestFloat(String prompt, Float min, Float max) {
        return requestNumber(prompt, min, max, "дробное число", Float::parseFloat);
    }

    /**
     * Запрашивает число типа Double.
     */
    public Double requestDouble(String prompt, Double min, Double max) {
        return requestNumber(prompt, min, max, "дробное число", Double::parseDouble);
    }

    /**
     * Запрашивает число типа long.
     */
    public long requestLong(String prompt, Long min, Long max) {
        return requestNumber(prompt, min, max, "целое число", Long::parseLong);
    }
}