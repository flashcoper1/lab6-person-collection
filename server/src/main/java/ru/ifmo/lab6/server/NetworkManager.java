package ru.ifmo.lab6.server;

import ru.ifmo.lab6.network.Request;
import ru.ifmo.lab6.network.Response;
import ru.ifmo.lab6.server.util.SerializationUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Управляет сетевым взаимодействием и другими событиями ввода-вывода на сервере с использованием NIO.
 * Работает в едином цикле событий (event loop), обрабатывая как сетевые запросы, так и консольные команды.
 */
public class NetworkManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());
    private static final int BUFFER_SIZE = 65536;

    private final int port;
    private final CommandExecutor commandExecutor;
    private DatagramChannel networkChannel;
    private Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private Consumer<String> consoleCommandHandler; // Обработчик для команд из консоли

    public NetworkManager(int port, CommandExecutor commandExecutor) {
        this.port = port;
        this.commandExecutor = commandExecutor;
    }

    // =============================================================
    // ===== ВОТ ДОБАВЛЕННЫЙ МЕТОД, КОТОРЫЙ РЕШАЕТ ПРОБЛЕМУ =====
    // =============================================================
    /**
     * Возвращает Selector, управляющий каналами.
     * Необходимо для вызова wakeup() из другого класса.
     * @return Selector.
     */
    public Selector getSelector() {
        return this.selector;
    }
    // =============================================================

    /**
     * Инициализирует Selector и сетевой канал.
     * @throws IOException если произошла ошибка при открытии ресурсов.
     */
    public void setup() throws IOException {
        selector = Selector.open();
        networkChannel = DatagramChannel.open();
        networkChannel.configureBlocking(false);
        networkChannel.socket().bind(new InetSocketAddress(port));
        networkChannel.register(selector, SelectionKey.OP_READ);
        LOGGER.info("Сетевой модуль готов. Сервер слушает порт " + port);
    }

    /**
     * Регистрирует канал для чтения команд с консоли сервера.
     * @param consoleSourceChannel Читающий конец Pipe.
     * @param handler Функция, которая будет вызвана с прочитанной командой.
     * @throws IOException если произошла ошибка регистрации канала.
     */
    public void registerConsoleChannel(Pipe.SourceChannel consoleSourceChannel, Consumer<String> handler) throws IOException {
        consoleSourceChannel.configureBlocking(false);
        consoleSourceChannel.register(selector, SelectionKey.OP_READ);
        this.consoleCommandHandler = handler;
        LOGGER.info("Канал для консольных команд успешно зарегистрирован.");
    }


    /**
     * Главный цикл обработки событий. Блокируется до тех пор, пока не появится
     * новое событие (сетевой пакет или консольная команда).
     */
    public void processEvents() throws IOException {
        // Теперь используем блокирующий select(), он сам "проснется", когда нужно.
        if (selector.select() > 0) {
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isReadable()) {
                    // Определяем, какой канал готов: сетевой или консольный
                    if (key.channel() == networkChannel) {
                        handleNetworkRead(key);
                    } else if (key.channel() instanceof Pipe.SourceChannel) {
                        handleConsoleRead(key);
                    }
                }
                iter.remove();
            }
        }
    }

    private void handleNetworkRead(SelectionKey key) {
        DatagramChannel clientChannel = (DatagramChannel) key.channel();
        buffer.clear();
        SocketAddress clientAddress;
        try {
            clientAddress = clientChannel.receive(buffer);
            if (clientAddress == null) return;

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

    private void handleConsoleRead(SelectionKey key) throws IOException {
        Pipe.SourceChannel consoleChannel = (Pipe.SourceChannel) key.channel();
        buffer.clear();
        int bytesRead = consoleChannel.read(buffer);
        if (bytesRead > 0) {
            buffer.flip();
            String command = new String(buffer.array(), 0, bytesRead).trim();
            if (!command.isEmpty()) {
                consoleCommandHandler.accept(command);
            }
        }
    }


    private void sendResponse(Response response, SocketAddress clientAddress) {
        try {
            byte[] responseData = SerializationUtil.serialize(response);
            ByteBuffer responseBuffer = ByteBuffer.wrap(responseData);
            networkChannel.send(responseBuffer, clientAddress);
            LOGGER.info("Отправлен ответ клиенту " + clientAddress);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Ошибка при отправке ответа клиенту " + clientAddress, e);
        }
    }

    public void close() {
        try {
            if (selector != null) selector.close();
            if (networkChannel != null) networkChannel.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при закрытии сетевых ресурсов", e);
        }
    }
}