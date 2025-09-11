package ru.ifmo.lab6.server.managers;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import ru.ifmo.lab6.model.Coordinates;
import ru.ifmo.lab6.model.Location;
import ru.ifmo.lab6.model.Person;
import ru.ifmo.lab6.util.LocalDateTimeAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Управляет загрузкой и сохранением коллекции в XML файл.
 */
public class XmlFileManager {
    private static final Logger LOGGER = Logger.getLogger(XmlFileManager.class.getName());
    private final String filePath;

    // Вспомогательный класс-обертка для корректной сериализации/десериализации TreeSet
    @XmlRootElement(name = "persons")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class PersonWrapper { // Добавлено ключевое слово 'static'
        @XmlElement(name = "person")
        private TreeSet<Person> persons = new TreeSet<>();

        public PersonWrapper() {}

        public PersonWrapper(TreeSet<Person> persons) {
            this.persons = persons;
        }

        public TreeSet<Person> getPersons() {
            return persons;
        }
    }

    public XmlFileManager(String filePath) {
        this.filePath = filePath.replace("\"", "");
    }

    public TreeSet<Person> load() {
        File file = new File(filePath);
        if (!file.exists()) {
            LOGGER.info("Файл коллекции не найден по пути: " + filePath + ". Будет создана новая коллекция.");
            return new TreeSet<>();
        }
        if (file.length() == 0) {
            LOGGER.info("Файл коллекции пуст. Создана новая коллекция.");
            return new TreeSet<>();
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JAXBContext context = JAXBContext.newInstance(PersonWrapper.class, Person.class, Coordinates.class, Location.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            PersonWrapper wrapper = (PersonWrapper) unmarshaller.unmarshal(reader);
            TreeSet<Person> loadedCollection = (wrapper != null && wrapper.getPersons() != null) ? wrapper.getPersons() : new TreeSet<>();
            LOGGER.info("Коллекция успешно загружена. Элементов: " + loadedCollection.size());
            return loadedCollection;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Критическая ошибка при загрузке коллекции из файла. Будет использована пустая коллекция.", e);
            return new TreeSet<>();
        }
    }

    public void save(TreeSet<Person> collection) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            JAXBContext context = JAXBContext.newInstance(PersonWrapper.class, Person.class, Coordinates.class, Location.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            PersonWrapper wrapper = new PersonWrapper(collection);
            marshaller.marshal(wrapper, writer);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Произошла ошибка при сохранении коллекции в файл!", e);
        }
    }
}