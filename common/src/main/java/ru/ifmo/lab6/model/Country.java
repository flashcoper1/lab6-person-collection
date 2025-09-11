package ru.ifmo.lab6.model;

import java.io.Serializable;

/**
 * Перечисление возможных стран.
 * Используется для национальности объекта Person.
 * Реализу "Serializable" для передачи по сети.
 */
public enum Country implements Serializable {
    INDIA("Индия"),
    VATICAN("Ватикан"),
    SOUTH_KOREA("Южная Корея");

    private final String russianName;

    /**
     * Конструктор для Enum.
     * @param russianName Название страны на русском языке для красивого вывода.
     */
    Country(String russianName) {
        this.russianName = russianName;
    }

    /**
     * Возвращает название страны на русском языке.
     * @return Строка с названием страны.
     */
    public String getRussianName() {
        return russianName;
    }

    /**
     * Статический метод для получения строки со всеми доступными значениями Enum.
     * Удобен для вывода пользователю в качестве подсказки.
     * @return Строка вида "INDIA (Индия), VATICAN (Ватикан), ..."
     */
    public static String listValues() {
        StringBuilder sb = new StringBuilder();
        for (Country country : values()) {
            sb.append(country.name())
                    .append(" (")
                    .append(country.getRussianName())
                    .append("), ");
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}