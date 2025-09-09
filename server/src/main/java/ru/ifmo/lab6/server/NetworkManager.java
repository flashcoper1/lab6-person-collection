package ru.ifmo.lab6.server;

import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;
import ru.ifmo.lab6.server.util.SerializationUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Управляет сетевым взаимодействием на сервере с использованием NIO (неблокирующий ввод-вывод).
 * Работает в одном потоке, обрабатывая все клиентские запросы.
 */
public class NetworkManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());
    private static final int BUFFER_SIZE = 65536; // 64KB, максимальный размер UDP пакета

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final int port;
    private final CommandExecutor commandExecutor;
    private DatagramChannel channel;
    private Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    public NetworkManager(int port, CommandExecutor commandExecutor) {
        this.port = port;
        this.commandExecutor = commandExecutor;
    }

    /**
     * Запускает сервер: открывает канал, настраивает его и входит в бесконечный цикл ожидания запросов.
     */
    public void run() {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ);

            LOGGER.info("Сервер успешно запущен на порту " + port);

            while (running.get()) {
                selector.select(); // Блокируется до появления события

                // Проверяем флаг после пробуждения
                if (!running.get()) {
                    break;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    // ...
                }
            }
        } catch (IOException e) {
            if (running.get()) { // Логируем ошибку, только если она не вызвана штатным закрытием
                LOGGER.log(Level.SEVERE, "Критическая сетевая ошибка", e);
            }
        } finally {
            close();
        }
        LOGGER.info("Сетевой менеджер завершил работу.");
    }

    public void stop() {
        running.set(false);
        if (selector != null) {
            selector.wakeup(); // "Будим" селектор, который может спать в select()
        }
    }

    private void handleRead(SelectionKey key) {
        DatagramChannel clientChannel = (DatagramChannel) key.channel();
        buffer.clear();
        SocketAddress clientAddress;
        try {
            clientAddress = clientChannel.receive(buffer);
            if (clientAddress == null) return; // Пакет мог быть потерян

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            LOGGER.info("Получен запрос от " + clientAddress);

            try {
                Request request = (Request) SerializationUtil.deserialize(data);
                Response response = commandExecutor.execute(request);
                sendResponse(response, clientAddress);
            } catch (ClassNotFoundException | ClassCastException e) {
                LOGGER.log(Level.WARNING, "Ошибка десериализации от " + clientAddress, e);
                sendResponse(new Response(Response.Status.ERROR, "Ошибка: неверный формат запроса."), clientAddress);
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Ошибка при чтении данных от клиента", e);
        }
    }

    private void sendResponse(Response response, SocketAddress clientAddress) {
        try {
            byte[] responseData = SerializationUtil.serialize(response);
            ByteBuffer responseBuffer = ByteBuffer.wrap(responseData);
            channel.send(responseBuffer, clientAddress);
            LOGGER.info("Отправлен ответ клиенту " + clientAddress);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Ошибка при отправке ответа клиенту " + clientAddress, e);
        }
    }

    /**
     * Корректно закрывает канал и селектор.
     */
    public void close() {
        try {
            if (selector != null) selector.close();
            if (channel != null) channel.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при закрытии сетевых ресурсов", e);
        }
    }
}