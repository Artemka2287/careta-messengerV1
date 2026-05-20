package com.careta.client;

import javax.swing.*;

public class ClientApp {
    public static void main(String[] args) {
        System.out.println("🚀 Запуск клиента...");

        SwingUtilities.invokeLater(() -> {
            System.out.println("🪟 Создание окна авторизации...");
            AuthFrame frame = new AuthFrame();
            System.out.println("✅ Окно создано, делаем видимым...");
            frame.setVisible(true);
            System.out.println("👁️ Окно должно быть видно!");
        });
    }
}