package com.careta.client;

import com.careta.common.CommandType;
import com.careta.common.NetworkPacket;

import com.careta.client.network.ClientSocket;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

// Окно авторизации: логин / регистрация / вход
public class AuthFrame extends JFrame {

    private JTextField tfNick;
    private JPasswordField pfPassword;
    private JButton btnLogin, btnRegister;
    private JLabel lblStatus;

    private ClientSocket clientSocket;

    public AuthFrame() {
        initUI();
        System.out.println("AuthFrame инициализирован");
        initSocket();
    }

    // Инициализация интерфейса
    private void initUI() {
        setTitle("🚕 Мессенджер «Карета» — Вход");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350, 250);
        setLocationRelativeTo(null); // По центру экрана
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Поле ника
        tfNick = new JTextField();
        JPanel nickPanel = new JPanel(new BorderLayout(5, 0));
        nickPanel.add(new JLabel("Ник:"), BorderLayout.WEST);
        nickPanel.add(tfNick, BorderLayout.CENTER);

        // Поле пароля
        pfPassword = new JPasswordField();
        JPanel passPanel = new JPanel(new BorderLayout(5, 0));
        passPanel.add(new JLabel("Пароль:"), BorderLayout.WEST);
        passPanel.add(pfPassword, BorderLayout.CENTER);

        // Кнопки
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnLogin = new JButton("Войти");
        btnRegister = new JButton("Регистрация");
        btnPanel.add(btnLogin);
        btnPanel.add(btnRegister);

        // Статус
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setForeground(Color.RED);

        panel.add(new JLabel("Авторизация", SwingConstants.CENTER));
        panel.add(nickPanel);
        panel.add(passPanel);
        panel.add(btnPanel);
        panel.add(lblStatus);

        add(panel);

        // Обработчики кнопок
        btnLogin.addActionListener(e -> attemptAuth(CommandType.LOGIN));
        btnRegister.addActionListener(e -> attemptAuth(CommandType.REGISTER));
    }

    // Инициализация сокета
    private void initSocket() {
        System.out.println("🔌 initSocket: начинаю подключение...");

        clientSocket = new ClientSocket();
        clientSocket.setOnPacketReceived(this::handleServerResponse);

        try {
            System.out.println("🔌 initSocket: вызываю connect()...");
            clientSocket.connect("localhost", 5001);
            System.out.println("🔌 initSocket: connect() завершён успешно!");

            lblStatus.setText("Подключено к серверу");
            lblStatus.setForeground(Color.GREEN);
        } catch (IOException e) {
            System.out.println("❌ initSocket: ошибка подключения: " + e.getMessage());
            e.printStackTrace();  // Покажет полный стек ошибки в консоли

            lblStatus.setText("Ошибка подключения!");
            JOptionPane.showMessageDialog(this, "Не удалось подключиться: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Попытка авторизации
    private void attemptAuth(CommandType type) {
        String nick = tfNick.getText().trim();
        String password = new String(pfPassword.getPassword());

        // Валидация: ник должен начинаться с буквы
        if (!nick.matches("^[a-zA-Z].*")) {
            lblStatus.setText("Ник должен начинаться с буквы");
            return;
        }

        // Отправляем пакет на сервер
        clientSocket.send(new NetworkPacket(type, new String[]{nick, password}));
        lblStatus.setText("Отправка...");
    }

    // Обработка ответа от сервера
    private void handleServerResponse(NetworkPacket packet) {
        SwingUtilities.invokeLater(() -> {
            switch (packet.getType()) {
                case AUTH_SUCCESS -> {
                    lblStatus.setText("Успешный вход!");
                    lblStatus.setForeground(Color.GREEN);
                    // Открываем окно чата и закрываем авторизацию
                    new ChatFrame(clientSocket, tfNick.getText()).setVisible(true);
                    dispose();
                }
                case AUTH_FAIL -> {
                    lblStatus.setText("Ошибка: неверный логин/пароль");
                    JOptionPane.showMessageDialog(this, "Неверный логин или пароль", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
                default -> System.out.println("⚠️ Неожиданный ответ: " + packet.getType());
            }
        });
    }
}