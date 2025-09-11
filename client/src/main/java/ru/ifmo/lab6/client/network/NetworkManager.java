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
        this.channel.register(selector, SelectionKey.OP_READ);
    }

    /**
     * Отправляет запрос на сервер и ожидает ответ.
     * @param request Объект запроса для отправки.
     * @return Объект ответа от сервера.
     * @throws IOException если произошел таймаут ожидания или другая сетевая ошибка.
     */
    public Response sendAndReceive(Request request) throws IOException {
        // 1. Сериализуем и отправляем запрос
        byte[] requestData = SerializationUtil.serialize(request);
        channel.send(ByteBuffer.wrap(requestData), serverAddress);
        System.out.println("-> Запрос (" + request.getCommandType() + ") отправлен на сервер.");

        // 2. Ждем ответа с таймаутом
        int readyChannels = selector.select(TIMEOUT_MS);

        if (readyChannels == 0) {
            // Если за TIMEOUT_MS не пришло ответа, бросаем исключение
            throw new IOException("Сервер не отвечает (таймаут " + TIMEOUT_MS + " мс).");
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            if (key.isReadable()) {
                buffer.clear();
                // Получаем датаграмму
                channel.receive(buffer);
                buffer.flip();

                byte[] responseData = new byte[buffer.remaining()];
                buffer.get(responseData);
                keyIterator.remove(); // Удаляем обработанный ключ

                try {
                    // Десериализуем ответ
                    Response response = (Response) SerializationUtil.deserialize(responseData);
                    System.out.println("<- Получен ответ от сервера.");
                    return response;
                } catch (ClassNotFoundException | ClassCastException e) {
                    // Эта ошибка означает, что клиент и сервер несовместимы
                    throw new IOException("Не удалось десериализовать ответ от сервера: " + e.getMessage(), e);
                }
            } else {
                keyIterator.remove();
            }
        }
        // Если по какой-то причине мы вышли из цикла, но не получили данные - это ошибка
        throw new IOException("Не удалось получить ответ от сервера после события чтения.");
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