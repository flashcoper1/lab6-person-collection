package ru.ifmo.lab6.server.managers;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import ru.ifmo.lab6.model.Person;
import ru.ifmo.lab6.util.LocalDateTimeAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Управляет загрузкой и сохранением коллекции в XML файл.
 * Адаптирован для серверной части.
 */
public class XmlFileManager {
    private static final Logger LOGGER = Logger.getLogger(XmlFileManager.class.getName());
    private final String filePath;

    @XmlRootElement(name = "persons")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class PersonWrapper {
        @XmlElement(name = "person")
        private TreeSet<Person> persons = new TreeSet<>();
        public PersonWrapper() {}
        public PersonWrapper(TreeSet<Person> persons) { this.persons = persons; }
        public TreeSet<Person> getPersons() { return persons; }
    }

    public XmlFileManager(String filePath) {
        this.filePath = filePath.replace("\"", "");
    }

    /**
     * Загружает коллекцию из XML файла.
     * @return Загруженная коллекция.
     */
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
            JAXBContext context = JAXBContext.newInstance(PersonWrapper.class, LocalDateTimeAdapter.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            PersonWrapper wrapper = (PersonWrapper) unmarshaller.unmarshal(reader);
            TreeSet<Person> loadedCollection = (wrapper != null && wrapper.getPersons() != null) ? wrapper.getPersons() : new TreeSet<>();
            LOGGER.info("Коллекция успешно загружена. Элементов: " + loadedCollection.size());
            return loadedCollection;
        } catch (FileNotFoundException e) {
            LOGGER.warning("Файл коллекции не найден: " + filePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка ввода-вывода при чтении файла: " + filePath, e);
        } catch (JAXBException e) {
            LOGGER.log(Level.SEVERE, "Ошибка парсинга XML файла. Файл может быть поврежден: " + filePath, e);
        } catch (SecurityException e) {
            LOGGER.log(Level.SEVERE, "Нет прав на чтение файла: " + filePath, e);
        }
        LOGGER.warning("Из-за ошибки загружена пустая коллекция.");
        return new TreeSet<>();
    }

    /**
     * Сохраняет коллекцию в XML файл.
     * @param collection Коллекция для сохранения.
     */
    public void save(TreeSet<Person> collection) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            JAXBContext context = JAXBContext.newInstance(PersonWrapper.class, LocalDateTimeAdapter.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            PersonWrapper wrapper = new PersonWrapper(collection);
            marshaller.marshal(wrapper, writer);
            LOGGER.info("Коллекция успешно сохранена в файл: " + filePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка ввода-вывода при записи в файл: " + filePath, e);
        } catch (JAXBException e) {
            LOGGER.log(Level.SEVERE, "Ошибка преобразования коллекции в XML", e);
        } catch (SecurityException e) {
            LOGGER.log(Level.SEVERE, "Нет прав на запись в файл: " + filePath, e);
        }
    }
}