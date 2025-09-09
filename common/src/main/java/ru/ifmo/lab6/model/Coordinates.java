package ru.ifmo.lab6.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Модель координат.
 * Реализует Serializable для передачи по сети.
 */
public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L; // Версия для сериализации

    private Double x; //Поле не может быть null, Максимальное значение поля: 348
    private float y;

    public Coordinates(Double x, float y) {
        this.setX(x);
        this.y = y;
    }

    /**
     * Конструктор по умолчанию для JAXB.
     */
    public Coordinates() {}

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        if (x == null) {
            throw new IllegalArgumentException("Координата X не может быть null.");
        }
        if (x > 348) {
            throw new IllegalArgumentException("Максимальное значение координаты X: 348.");
        }
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Coordinates{x=" + x + ", y=" + y + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return Float.compare(that.y, y) == 0 && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}