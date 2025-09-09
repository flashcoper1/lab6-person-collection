package ru.ifmo.lab6.server.managers;

import ru.ifmo.lab6.model.Color;
import ru.ifmo.lab6.model.Person;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Управляет коллекцией объектов Person.
 * Все операции по обработке коллекции реализованы с использованием Stream API.
 */
public class CollectionManager {
    private TreeSet<Person> collection;
    private final ZonedDateTime initializationTime;
    private long nextId = 1;

    public CollectionManager(TreeSet<Person> initialCollection) {
        this.initializationTime = ZonedDateTime.now();
        this.collection = Objects.requireNonNullElse(initialCollection, new TreeSet<>());
        updateNextId();
    }

    private void updateNextId() {
        nextId = collection.stream()
                .mapToLong(Person::getId)
                .max()
                .orElse(0L) + 1;
    }

    public synchronized TreeSet<Person> getCollection() {
        return collection;
    }

    public synchronized String getInfo() {
        return "Тип коллекции: " + collection.getClass().getName() +
                "\nДата инициализации: " + initializationTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z")) +
                "\nКоличество элементов: " + collection.size();
    }

    public synchronized String add(Person person) {
        person.setId(nextId++);
        person.setCreationDate(java.time.LocalDateTime.now());
        collection.add(person);
        return "Новый человек успешно добавлен с ID: " + person.getId();
    }

    public synchronized String addIfMin(Person person) {
        Optional<Person> minPerson = collection.stream().min(Person::compareTo);
        if (minPerson.isEmpty() || person.compareTo(minPerson.get()) < 0) {
            return add(person);
        }
        return "Элемент не был добавлен, так как он не меньше минимального.";
    }

    public synchronized String update(long id, Person updatedPersonData) {
        Optional<Person> personOptional = collection.stream().filter(p -> p.getId() == id).findFirst();
        if (personOptional.isPresent()) {
            Person personToUpdate = personOptional.get();
            collection.remove(personToUpdate);

            updatedPersonData.setId(id);
            updatedPersonData.setCreationDate(personToUpdate.getCreationDate());
            collection.add(updatedPersonData);
            return "Человек с ID " + id + " успешно обновлен.";
        }
        return "Человек с ID " + id + " не найден.";
    }

    public synchronized String removeById(long id) {
        boolean removed = collection.removeIf(person -> person.getId() == id);
        return removed ? "Человек с ID " + id + " успешно удален." : "Человек с ID " + id + " не найден.";
    }

    public synchronized String clear() {
        collection.clear();
        nextId = 1;
        return "Коллекция успешно очищена.";
    }

    public synchronized String removeGreater(Person person) {
        int initialSize = collection.size();
        collection = collection.stream()
                .filter(p -> p.compareTo(person) <= 0)
                .collect(Collectors.toCollection(TreeSet::new));
        int removedCount = initialSize - collection.size();
        return "Удалено " + removedCount + " элементов, больших чем заданный.";
    }

    public synchronized String removeLower(Person person) {
        int initialSize = collection.size();
        collection = collection.stream()
                .filter(p -> p.compareTo(person) >= 0)
                .collect(Collectors.toCollection(TreeSet::new));
        int removedCount = initialSize - collection.size();
        return "Удалено " + removedCount + " элементов, меньших чем заданный.";
    }

    public synchronized double getAverageHeight() {
        return collection.stream()
                .mapToLong(Person::getHeight)
                .average()
                .orElse(0.0);
    }

    public synchronized long countByHairColor(Color hairColor) {
        return collection.stream()
                .filter(p -> Objects.equals(p.getHairColor(), hairColor))
                .count();
    }

    public synchronized TreeSet<Person> filterLessThanHairColor(Color hairColor) {
        if (hairColor == null) return new TreeSet<>();
        return collection.stream()
                .filter(p -> p.getHairColor() != null && p.getHairColor().ordinal() < hairColor.ordinal())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}