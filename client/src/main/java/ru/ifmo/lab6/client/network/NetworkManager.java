package ru.ifmo.lab6.client.network;

import ru.ifmo.lab6.client.network.util.SerializationUtil;
import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * Управляет сетевым взаимодействием на клиенте с использованием NIO.
 * Отправляет запросы и ожидает ответа от сервера с таймаутом.
 */
public class NetworkManager {
    private static final int BUFFER_SIZE = 65536;
    private static final int TIMEOUT_MS = 5000;

    private final InetSocketAddress serverAddress;
    private final DatagramChannel channel;
    private final Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    public NetworkManager(String host, int port) throws IOException {
        this.serverAddress = new InetSocketAddress(host, port);
        this.selector = Selector.open();
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.register(selector, SelectionKey.OP_READ); // Регистрируем один раз при создании
    }

    public Response sendAndReceive(Request request) throws IOException {
        // 1. Отправляем запрос
        byte[] requestData = SerializationUtil.serialize(request);
        channel.send(ByteBuffer.wrap(requestData), serverAddress);
        System.out.println("-> Запрос отправлен на сервер.");

        // 2. Ждем ответа
        int readyChannels = selector.select(TIMEOUT_MS);

        if (readyChannels == 0) {
            // Таймаут
            System.err.println("Ошибка: Сервер не отвечает. Попробуйте позже.");
            return null;
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        // Хотя ключ должен быть один, проходим по всем на всякий случай
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            if (key.isReadable()) {
                buffer.clear();
                // Используем receive, а не read, так как канал не "подключен" постоянно
                channel.receive(buffer);
                buffer.flip();

                byte[] responseData = new byte[buffer.remaining()];
                buffer.get(responseData);

                keyIterator.remove(); // Удаляем обработанный ключ

                try {
                    Response response = (Response) SerializationUtil.deserialize(responseData);
                    System.out.println("<- Получен и успешно десериализован ответ от сервера.");
                    return response;
                } catch (ClassNotFoundException | ClassCastException e) {
                    System.err.println("!!! КРИТИЧЕСКАЯ ОШИБКА: Не удалось десериализовать ответ от сервера.");
                    System.err.println("!!! Причина: " + e.getMessage());
                    // e.printStackTrace(); // Можно раскомментировать для полной отладки
                    return null;
                }
            } else {
                keyIterator.remove();
            }
        }
        return null; // Если по какой-то причине не удалось прочитать
    }

    public void close() {
        try {
            if (selector != null) selector.close();
            if (channel != null) channel.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии сетевых ресурсов клиента: " + e.getMessage());
        }
    }
}