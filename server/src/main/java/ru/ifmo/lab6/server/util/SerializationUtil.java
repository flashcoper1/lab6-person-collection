package ru.ifmo.lab6.server.util;

import java.io.*;

/**
 * Утилитарный класс для сериализации и десериализации объектов.
 */
public class SerializationUtil {

    /**
     * Сериализует объект в массив байт.
     * @param obj Объект для сериализации.
     * @return Массив байт.
     * @throws IOException если произошла ошибка ввода-вывода.
     */
    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        }
    }

    /**
     * Десериализует объект из массива байт.
     * @param bytes Массив байт.
     * @return Десериализованный объект.
     * @throws IOException если произошла ошибка ввода-вывода.
     * @throws ClassNotFoundException если класс объекта не найден.
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }
}