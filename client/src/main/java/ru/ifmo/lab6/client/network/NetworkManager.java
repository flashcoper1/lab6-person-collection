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
    private static final int TIMEOUT_MS = 5000; // 5 секунд

    private final InetSocketAddress serverAddress;
    private DatagramChannel channel;
    private Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    public NetworkManager(String host, int port) throws IOException {
        this.serverAddress = new InetSocketAddress(host, port);
        this.selector = Selector.open();
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.connect(serverAddress); // connect() позволяет использовать read/write
    }

    /**
     * Отправляет запрос на сервер и получает ответ.
     * @param request Запрос для отправки.
     * @return Ответ от сервера.
     * @throws IOException если произошла сетевая ошибка.
     * @throws ClassNotFoundException если ответ от сервера имеет неизвестный класс.
     */
    public Response sendAndReceive(Request request) throws IOException, ClassNotFoundException {
        // 1. Отправляем запрос
        byte[] requestData = SerializationUtil.serialize(request);
        channel.send(ByteBuffer.wrap(requestData), serverAddress);
        System.out.println("-> Запрос отправлен на сервер.");

        // 2. Ждем ответа
        channel.register(selector, SelectionKey.OP_READ);
        int readyChannels = selector.select(TIMEOUT_MS);

        if (readyChannels == 0) {
            // Таймаут
            System.err.println("Ошибка: Сервер не отвечает. Попробуйте позже.");
            return null;
        }

        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            if (key.isReadable()) {
                buffer.clear();
                channel.receive(buffer);
                buffer.flip();

                byte[] responseData = new byte[buffer.remaining()];
                buffer.get(responseData);

                System.out.println("<- Получен ответ от сервера.");
                return (Response) SerializationUtil.deserialize(responseData);
            }
            keyIterator.remove();
        }
        return null; // Не должно произойти, если select > 0
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