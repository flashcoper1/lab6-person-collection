package ru.ifmo.lab6.client.util;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Реализация InputProvider для чтения из файла скрипта с помощью Scanner.
 */
public class ScriptInputProvider implements InputProvider {
    private final Scanner scanner;

    public ScriptInputProvider(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public String readLine(String prompt) throws NoSuchElementException {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            System.out.println(line);
            return line;
        } else {
            throw new NoSuchElementException("Конец файла скрипта.");
        }
    }
}