package ru.ifmo.lab6.model;

import java.io.Serializable;

/**
 * Перечисление возможных цветов.
 * Используется для цвета глаз и волос объекта Person.
 * Реализует Serializable для передачи по сети.
 */
public enum Color implements Serializable {
    RED("Красный"),
    YELLOW("Желтый"),
    GREEN("Зеленый"),
    BLUE("Синий"),
    WHITE("Белый"),
    BROWN("Коричневый");

    private final String russianName;

    /**
     * Конструктор для Enum.
     * @param russianName Название цвета на русском языке для красивого вывода.
     */
    Color(String russianName) {
        this.russianName = russianName;
    }

    /**
     * Возвращает название цвета на русском языке.
     * @return Строка с названием цвета.
     */
    public String getRussianName() {
        return russianName;
    }

    /**
     * Статический метод для получения строки со всеми доступными значениями Enum.
     * Удобен для вывода пользователю в качестве подсказки.
     * @return Строка вида "RED (Красный), YELLOW (Желтый), ..."
     */
    public static String listValues() {
        StringBuilder sb = new StringBuilder();
        for (Color color : values()) {
            sb.append(color.name())
                    .append(" (")
                    .append(color.getRussianName())
                    .append("), ");
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}