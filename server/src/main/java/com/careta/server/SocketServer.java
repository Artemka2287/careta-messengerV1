package com.careta.server;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SocketServer {

    private static final int PORT = 5001;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final ClientHandler.Factory clientHandlerFactory;

    public SocketServer(ClientHandler.Factory clientHandlerFactory) {
        System.out.println("⚙️ SocketServer: конструктор вызван");
        System.out.flush(); // <-- ВАЖНО: сброс буфера
        this.clientHandlerFactory = clientHandlerFactory;
    }

    @PostConstruct
    public void start() {
        System.out.println("⚙️ SocketServer: @PostConstruct, запускаю поток...");
        System.out.flush();
        new Thread(this::runServer, "SocketServer-Thread").start();
    }

    private void runServer() {
        System.out.println("⚙️ SocketServer: поток runServer() стартовал");
        System.out.flush();

        threadPool = Executors.newCachedThreadPool();

        try {
            System.out.println("⚙️ SocketServer: создаю ServerSocket на порту " + PORT);
            System.out.flush();

            serverSocket = new ServerSocket(PORT);

            System.out.println("📡 Сервер слушает порт " + PORT);
            System.out.flush(); // <-- КРИТИЧНО: чтобы сообщение появилось сразу

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Подключился клиент: " + clientSocket.getInetAddress());
                System.out.flush();

                ClientHandler handler = clientHandlerFactory.create(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("❌ Ошибка сервера: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
        }
    }

    @PreDestroy
    public void stop() {
        System.out.println("🛑 Остановка сервера...");
        System.out.flush();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при остановке: " + e.getMessage());
        }
    }
}