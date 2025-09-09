package ru.ifmo.lab6.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
/**
 * Модель местоположения.
 * Реализует Serializable для передачи по сети.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Location implements Serializable {
    private static final long serialVersionUID = 2L;

    private Float x; //Поле не может быть null
    private double y;
    private Double z; //Поле не может быть null
    private String name; //Длина строки не должна быть больше 400, Поле может быть null

    public Location(Float x, double y, Double z, String name) {
        this.setX(x);
        this.y = y;
        this.setZ(z);
        this.setName(name);
    }
    /**
     * Конструктор по умолчанию для JAXB.
     */
    public Location() {}

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        if (x == null) {
            throw new IllegalArgumentException("Координата X местоположения не может быть null.");
        }
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        if (z == null) {
            throw new IllegalArgumentException("Координата Z местоположения не может быть null.");
        }
        this.z = z;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && name.length() > 400) {
            throw new IllegalArgumentException("Длина названия местоположения не должна превышать 400 символов.");
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return "Location{" +
                "name='" + (name == null ? "N/A" : name) + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Double.compare(location.y, y) == 0 && Objects.equals(x, location.x) && Objects.equals(z, location.z) && Objects.equals(name, location.name);
    }



    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, name);
    }
}