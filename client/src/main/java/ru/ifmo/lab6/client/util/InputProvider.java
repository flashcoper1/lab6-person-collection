package ru.ifmo.lab6.client.util;

import java.util.NoSuchElementException;

/**
 * Абстрактный интерфейс для поставщика ввода.
 * Позволяет UserInputHandler читать данные из разных источников (консоль, файл)
 * без изменения своей логики.
 */
public interface InputProvider {
    /**
     * Читает следующую строку из источника.
     * @param prompt Приглашение к вводу (актуально только для консоли).
     * @return Прочитанная строка.
     * @throws NoSuchElementException если ввод был прерван или источник исчерпан.
     */
    String readLine(String prompt) throws NoSuchElementException;
}