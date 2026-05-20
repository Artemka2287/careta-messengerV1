package com.careta.client;

import com.careta.client.network.ClientSocket;
import com.careta.common.CommandType;
import com.careta.common.NetworkPacket;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import com.careta.common.MessageDTO;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Окно чата — основной интерфейс пользователя после авторизации.
 * Реализует: список онлайн-пользователей, отправку сообщений, получение в реальном времени.
 */
public class ChatFrame extends JFrame {

    private final ClientSocket clientSocket;
    private final String myNick;

    // Формат времени для отображения в чате
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    // UI-компоненты
    private JList<String> usersList;          // Список подключённых пользователей
    private DefaultListModel<String> usersModel;
    private JTextArea chatArea;               // Область отображения сообщений
    private JTextField inputField;            // Поле ввода сообщения
    private JButton sendButton;               // Кнопка отправки
    private JLabel statusLabel;               // Статус: кому отправляем

    // Текущий выбранный собеседник (null = всем)
    private String selectedUser = null;

    /**
     * Конструктор: инициализирует чат для пользователя myNick.
     * @param clientSocket активное соединение с сервером
     * @param nick ник текущего пользователя
     */
    public ChatFrame(ClientSocket clientSocket, String nick) {
        this.clientSocket = clientSocket;
        this.myNick = nick;

        initUI();  // Создаём интерфейс
        initSocketListener();  // Настраиваем приём ответов от сервера

        // Запрашиваем актуальный список онлайн-пользователей
        clientSocket.send(new NetworkPacket(CommandType.GET_ONLINE, null));
    }

    /**
     * Инициализация графического интерфейса (Swing)
     */
    private void initUI() {
        // === Настройки окна ===
        setTitle("🚕 Чат — " + myNick);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null); // По центру экрана
        setMinimumSize(new Dimension(600, 400));

        // === 1. Левая панель: список пользователей ===
        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);
        usersList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Обработчик двойного клика: выбор собеседника + загрузка истории
        usersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {  // Двойной клик
                    int index = usersList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        selectedUser = usersModel.get(index);
                        // Если выбрано "Всем" — сбрасываем выбор
                        if (selectedUser.equals("📢 Всем")) {
                            selectedUser = null;
                        }
                        updateStatus();
                        loadHistory();  // <-- Загружаем историю при выборе!
                    }
                }
            }
        });

        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setPreferredSize(new Dimension(180, 0));
        usersScroll.setBorder(BorderFactory.createTitledBorder("Онлайн"));

        // === 2. Центральная панель: область чата ===
        chatArea = new JTextArea();
        chatArea.setEditable(false);  // Только чтение
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Переписка"));

        // === 3. Нижняя панель: ввод сообщения ===
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));

        sendButton = new JButton("Отправить");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        sendButton.addActionListener(e -> sendMessage());

        // Отправка по нажатию Enter
        inputField.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // === 4. Статусная строка ===
        statusLabel = new JLabel("📢 Отправка: всем", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);

        // === Поле поиска (требование №10) ===
        JTextField searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setToolTipText("Введите слово для поиска");

        JButton searchBtn = new JButton("🔍");
        searchBtn.setToolTipText("Поиск по сообщениям");

        // Обработчик кнопки поиска
        searchBtn.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty() || selectedUser == null) {
                JOptionPane.showMessageDialog(this,
                        "Выберите собеседника и введите слово для поиска",
                        "Поиск", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            chatArea.setText("🔍 Поиск \"" + keyword + "\"...\n");
            clientSocket.send(new NetworkPacket(CommandType.SEARCH_MESSAGES,
                    new String[]{myNick, selectedUser, keyword}));
        });

        // Панель поиска: поле + кнопка
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // === Сборка интерфейса ===
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(usersScroll, BorderLayout.WEST);

        // Центральная панель: поиск сверху, чат ниже
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        centerPanel.add(searchPanel, BorderLayout.NORTH); // <-- Поле поиска
        centerPanel.add(chatScroll, BorderLayout.CENTER); // <-- Область чата
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Нижняя панель: ввод сообщения + статус
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.add(inputPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Устанавливаем фокус на поле ввода при открытии
        add(mainPanel);
        inputField.requestFocusInWindow();
    }

    /**
     * Настраивает обработчик входящих пакетов от сервера.
     * Все обновления UI выполняются в потоке Swing (EDT) через invokeLater.
     */
    private void initSocketListener() {
        clientSocket.setOnPacketReceived(packet -> {
            // ВАЖНО: обновления Swing-компонентов только в EDT-потоке!
            SwingUtilities.invokeLater(() -> handleServerResponse(packet));
        });
    }

    /**
     * Запрашивает историю переписки с выбранным пользователем.
     */
    private void loadHistory() {
        if (selectedUser == null) {
            chatArea.setText("📢 Общий чат (история не сохраняется)\n");
            return;
        }
        chatArea.setText("⏳ Загрузка истории...\n");
        clientSocket.send(new NetworkPacket(CommandType.GET_HISTORY,
                new String[]{myNick, selectedUser}));
    }

    /**
     * Обрабатывает ответы сервера: онлайн-список, новые сообщения, ошибки.
     */
    /**
     * Обрабатывает ответы сервера: онлайн-список, новые сообщения, история, поиск.
     */
    private void handleServerResponse(NetworkPacket packet) {
        switch (packet.getType()) {

            // === Обновление списка онлайн-пользователей ===
            case ONLINE_UPDATE -> {
                @SuppressWarnings("unchecked")
                List<String> onlineUsers = (List<String>) packet.getPayload();
                updateUsersList(onlineUsers);
            }

            // === Получено новое сообщение (в реальном времени) ===
            case RECEIVE_MESSAGE -> {
                String[] data = (String[]) packet.getPayload();
                String from = data[0];
                String to = data[1];
                String text = data[2];
                appendMessage(formatMessage(from, to, text, "sent"));
            }

            // === ИСТОРИЯ ЧАТА (Требование №5) ===
            case HISTORY_RESPONSE -> {
                @SuppressWarnings("unchecked")
                List<MessageDTO> history = (List<MessageDTO>) packet.getPayload();
                chatArea.setText(""); // Очищаем чат перед загрузкой

                if (history.isEmpty()) {
                    chatArea.append("📭 Нет сообщений с " + selectedUser + "\n");
                } else {
                    for (MessageDTO msg : history) {
                        // Берём реальное время из БД
                        String time = msg.getSentAt().format(TIME_FORMAT);
                        appendMessage(formatMessage(msg.getSender(), msg.getReceiver(), msg.getText(), time));
                    }
                }
            }

            // === РЕЗУЛЬТАТЫ ПОИСКА (Требование №10) ===
            case SEARCH_RESPONSE -> {
                @SuppressWarnings("unchecked")
                List<MessageDTO> results = (List<MessageDTO>) packet.getPayload();
                chatArea.setText("🔍 Результаты поиска:\n");

                if (results.isEmpty()) {
                    chatArea.append("❌ Ничего не найдено\n");
                } else {
                    for (MessageDTO msg : results) {
                        String time = msg.getSentAt().format(TIME_FORMAT);
                        appendMessage(formatMessage(msg.getSender(), msg.getReceiver(), msg.getText(), time));
                    }
                }
            }

            // === Ошибка авторизации ===
            case AUTH_FAIL -> {
                String error = (String) packet.getPayload();
                JOptionPane.showMessageDialog(this,
                        "Ошибка сервера: " + error, "Ошибка", JOptionPane.ERROR_MESSAGE);
            }

            // === Неизвестная команда ===
            default -> System.out.println("⚠️ ChatFrame: неизвестный тип пакета: " + packet.getType());
        }
    }

    /**
     * Обновляет список пользователей в JList.
     * Исключает текущего пользователя из списка.
     */
    private void updateUsersList(List<String> users) {
        usersModel.clear();

        // Добавляем пункт "📢 Всем" для отправки общего сообщения
        usersModel.addElement("📢 Всем");

        for (String user : users) {
            // Не показываем себя в списке собеседников
            if (!user.equalsIgnoreCase(myNick)) {
                usersModel.addElement(user);
            }
        }

        // Если выбранный пользователь больше не онлайн — сбрасываем выбор
        if (selectedUser != null && !users.contains(selectedUser)) {
            selectedUser = null;
            updateStatus();
        }
    }

    /**
     * Отправляет сообщение на сервер.
     * Формат: [отправитель, получатель (null=всем), текст]
     */
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;  // Не отправляем пустые сообщения

        // Определяем получателя
        String receiver = selectedUser;
        // Если выбрано "Всем" или ничего не выбрано — отправляем всем
        if (receiver == null || receiver.equals("📢 Всем")) {
            receiver = null;
        }

        // Формируем и отправляем пакет
        NetworkPacket packet = new NetworkPacket(CommandType.SEND_MESSAGE,
                new String[]{myNick, receiver, text});
        clientSocket.send(packet);

        // Отображаем своё сообщение в чате сразу (оптимистичный UI)
        appendMessage(formatMessage(myNick, receiver, text, "sent"));

        // Очищаем поле ввода
        inputField.setText("");
        inputField.requestFocusInWindow();
    }

    /**
     * Добавляет сообщение в область чата с прокруткой вниз.
     * Вызывается ТОЛЬКО из EDT-потока (SwingUtilities.invokeLater).
     */
    private void appendMessage(String message) {
        chatArea.append(message + "\n");
        // Автопрокрутка вниз
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * Форматирует сообщение для отображения в чате.
     */
    private String formatMessage(String from, String to, String text, String timeOrStatus) {
        StringBuilder sb = new StringBuilder();

        // Время: если это формат HH:mm — показываем его, иначе — текущее время
        if (timeOrStatus.matches("\\d{2}:\\d{2}")) {
            sb.append("[").append(timeOrStatus).append("] ");
        } else {
            sb.append("[").append(LocalTime.now().format(TIME_FORMAT)).append("] ");
        }

        // Отправитель
        if (from.equals(myNick)) {
            sb.append("📤 Вы");
        } else {
            sb.append("📥 ").append(from);
        }

        // Получатель (для личных сообщений)
        if (to != null && !to.isEmpty() && !to.equals("null")) {
            sb.append(" → ").append(to);
        }

        // Текст
        sb.append(": ").append(text);

        // Галочка, если это только что отправленное сообщение
        if ("sent".equals(timeOrStatus)) {
            sb.append(" ✓");
        }

        return sb.toString();
    }

    /**
     * Обновляет статусную строку: кому отправляем сообщения.
     */
    private void updateStatus() {
        if (selectedUser == null || selectedUser.equals("📢 Всем")) {
            statusLabel.setText("📢 Отправка: всем");
            statusLabel.setForeground(Color.GRAY);
        } else {
            statusLabel.setText("💬 Личное: " + selectedUser);
            statusLabel.setForeground(Color.BLUE);
        }
    }

    /**
     * Геттер для получения текущего выбранного собеседника.
     * Может использоваться для загрузки истории чата (расширение).
     */
    public String getSelectedUser() {
        return selectedUser;
    }
}