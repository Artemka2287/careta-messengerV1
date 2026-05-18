package com.careta.client.network;

import com.careta.common.NetworkPacket;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientSocket {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<NetworkPacket> onPacketReceived;

    public void connect(String host, int port) throws IOException {
        System.out.println("🔌 ClientSocket: соединяюсь с " + host + ":" + port);
        System.out.flush();

        socket = new Socket(host, port);
        System.out.println("🔌 ClientSocket: Socket создан");
        System.out.flush();

        out = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("🔌 ClientSocket: ObjectOutputStream создан");
        System.out.flush();
        out.flush();  // <-- КРИТИЧНО
        System.out.println("🔌 ClientSocket: flush() выполнен");
        System.out.flush();

        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("🔌 ClientSocket: ObjectInputStream создан");
        System.out.flush();

        new Thread(this::readLoop, "ClientSocket-Reader").start();
        System.out.println("🔌 ClientSocket: поток чтения запущен");
        System.out.flush();
    }

    public void send(NetworkPacket packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            System.err.println("❌ Ошибка отправки: " + e.getMessage());
        }
    }

    private void readLoop() {
        try {
            while (!socket.isClosed()) {
                NetworkPacket packet = (NetworkPacket) in.readObject();
                if (onPacketReceived != null) {
                    onPacketReceived.accept(packet);
                }
            }
        } catch (EOFException e) {
            System.out.println("🔌 Сервер отключился");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Ошибка чтения: " + e.getMessage());
        }
    }

    public void setOnPacketReceived(Consumer<NetworkPacket> handler) {
        this.onPacketReceived = handler;
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Ошибка закрытия: " + e.getMessage());
        }
    }
}