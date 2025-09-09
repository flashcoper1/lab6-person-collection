package ru.ifmo.lab6.command;

import ru.ifmo.lab6.model.Color;
import ru.ifmo.lab6.model.Person;

import java.io.Serializable;

/**
 * Вспомогательный класс-пространство имен для хранения DTO (Data Transfer Objects)
 * для аргументов команд. Все внутренние классы должны быть Serializable.
 */
public final class Command implements Serializable {
    private static final long serialVersionUID = 201L;

    private Command() {
        // Утилитарный класс не должен инстанциироваться
    }

    public static class Add implements Serializable {
        private static final long serialVersionUID = 202L;
        public final Person person;
        public Add(Person person) { this.person = person; }
    }

    public static class Update implements Serializable {
        private static final long serialVersionUID = 203L;
        public final long id;
        public final Person person;
        public Update(long id, Person person) { this.id = id; this.person = person; }
    }

    public static class RemoveById implements Serializable {
        private static final long serialVersionUID = 204L;
        public final long id;
        public RemoveById(long id) { this.id = id; }
    }

    public static class AddIfMin implements Serializable {
        private static final long serialVersionUID = 205L;
        public final Person person;
        public AddIfMin(Person person) { this.person = person; }
    }

    public static class RemoveGreater implements Serializable {
        private static final long serialVersionUID = 206L;
        public final Person person;
        public RemoveGreater(Person person) { this.person = person; }
    }

    public static class RemoveLower implements Serializable {
        private static final long serialVersionUID = 207L;
        public final Person person;
        public RemoveLower(Person person) { this.person = person; }
    }

    public static class CountByHairColor implements Serializable {
        private static final long serialVersionUID = 208L;
        public final Color hairColor;
        public CountByHairColor(Color hairColor) { this.hairColor = hairColor; }
    }

    public static class FilterLessThanHairColor implements Serializable {
        private static final long serialVersionUID = 209L;
        public final Color hairColor;
        public FilterLessThanHairColor(Color hairColor) { this.hairColor = hairColor; }
    }

    public static class ExecuteScript implements Serializable {
        private static final long serialVersionUID = 210L;
        public final String fileName;
        public ExecuteScript(String fileName) { this.fileName = fileName; }
    }
}