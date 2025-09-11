package ru.ifmo.lab6.client.util;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import java.util.NoSuchElementException;

/**
 * Реализация InputProvider для чтения из интерактивной консоли с помощью JLine.
 */
public class ConsoleInputProvider implements InputProvider {
    private final LineReader lineReader;

    public ConsoleInputProvider(LineReader lineReader) {
        this.lineReader = lineReader;
    }

    @Override
    public String readLine(String prompt) throws NoSuchElementException {
        try {
            return this.lineReader.readLine(prompt + " ");
        } catch (UserInterruptException e) {
            throw new NoSuchElementException("Ввод был прерван пользователем.");
        } catch (EndOfFileException e) {
            throw new NoSuchElementException("Достигнут конец потока ввода.");
        }
    }
}