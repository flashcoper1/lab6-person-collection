package ru.ifmo.lab6.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import ru.ifmo.lab6.util.LocalDateTimeAdapter;
/**
 * Основной класс, объекты которого хранятся в коллекции.
 * Реализует Comparable для сортировки и Serializable для передачи по сети.
 */
public class Person implements Comparable<Person>, Serializable {
    private static final long serialVersionUID = 3L;

    private long id;
    private String name;
    private Coordinates coordinates;
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private java.time.LocalDateTime creationDate;
    private long height;
    private Color eyeColor;
    private Color hairColor;
    private Country nationality;
    private Location location;

    /**
     * Конструктор по умолчанию (без аргументов).
     * КРАЙНЕ ВАЖЕН для корректной работы фреймворков типа JAXB,
     * которые создают объект, а затем заполняют его поля через сеттеры или рефлексию.
     */
    public Person() {}

    /**
     * Конструктор для создания объекта (например, на клиенте).
     * ID и дата создания будут установлены сервером.
     */
    public Person(String name, Coordinates coordinates, long height, Color eyeColor, Color hairColor, Country nationality, Location location) {
        this.setName(name);
        this.setCoordinates(coordinates);
        this.setHeight(height);
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.nationality = nationality;
        this.setLocation(location);
    }

    public long getId() { return id; }
    public void setId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть больше 0.");
        }
        this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть null или пустым.");
        }
        this.name = name;
    }

    public Coordinates getCoordinates() { return coordinates; }
    public void setCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Координаты не могут быть null.");
        }
        this.coordinates = coordinates;
    }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) {
        if (creationDate == null) {
            throw new IllegalArgumentException("Дата создания не может быть null.");
        }
        this.creationDate = creationDate;
    }

    public long getHeight() { return height; }
    public void setHeight(long height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Рост должен быть больше 0.");
        }
        this.height = height;
    }

    public Color getEyeColor() { return eyeColor; }
    public void setEyeColor(Color eyeColor) { this.eyeColor = eyeColor; }
    public Color getHairColor() { return hairColor; }
    public void setHairColor(Color hairColor) { this.hairColor = hairColor; }
    public Country getNationality() { return nationality; }
    public void setNationality(Country nationality) { this.nationality = nationality; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Местоположение не может быть null.");
        }
        this.location = location;
    }

    @Override
    public int compareTo(Person other) {
        return Long.compare(this.id, other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return id == person.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return "Person {\n" +
                "  id=" + id + ",\n" +
                "  name='" + name + "',\n" +
                "  coordinates=" + coordinates + ",\n" +
                "  creationDate=" + (creationDate != null ? creationDate.format(formatter) : "N/A") + ",\n" +
                "  height=" + height + ",\n" +
                "  eyeColor=" + (eyeColor != null ? eyeColor.getRussianName() : "N/A") + ",\n" +
                "  hairColor=" + (hairColor != null ? hairColor.getRussianName() : "N/A") + ",\n" +
                "  nationality=" + (nationality != null ? nationality.getRussianName() : "N/A") + ",\n" +
                "  location=" + location + "\n" +
                '}';
    }
}