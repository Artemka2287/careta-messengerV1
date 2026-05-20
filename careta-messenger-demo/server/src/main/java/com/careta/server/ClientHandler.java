package com.careta.server;

import com.careta.common.MessageDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.careta.common.CommandType;
import com.careta.common.NetworkPacket;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.Socket;
import java.util.Optional;
import com.careta.server.MessageStatus;





public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected = true;
    private String currentNick; // Ник после успешной авторизации

    // Зависимости (внедряются через Factory)
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SessionRegistry sessionRegistry;

    @Component
    public static class Factory {
        private final UserRepository userRepository;
        private final MessageRepository messageRepository;
        private final SessionRegistry sessionRegistry;

        public Factory(UserRepository userRepository,
                       MessageRepository messageRepository,
                       SessionRegistry sessionRegistry) {
            this.userRepository = userRepository;
            this.messageRepository = messageRepository;
            this.sessionRegistry = sessionRegistry;
        }

        public ClientHandler create(Socket socket) {
            return new ClientHandler(socket, userRepository, messageRepository, sessionRegistry);
        }
    }

    public ClientHandler(Socket socket, UserRepository userRepository,
                         MessageRepository messageRepository, SessionRegistry sessionRegistry) {
        this.socket = socket;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (connected) {
                NetworkPacket packet = (NetworkPacket) in.readObject();
                processPacket(packet);
            }
        } catch (EOFException e) {
            System.out.println(" Клиент отключился: " + currentNick);
        } catch (Exception e) {
            System.err.println("❌ Ошибка связи: " + e.getMessage());
        } finally {
            // При отключении удаляем из онлайна
            sessionRegistry.removeSession(currentNick);
            close();
        }
    }

    private void processPacket(NetworkPacket packet) {
        switch (packet.getType()) {
            case LOGIN -> handleLogin((String[]) packet.getPayload());
            case REGISTER -> handleRegister((String[]) packet.getPayload());
            case SEND_MESSAGE -> handleSendMessage((String[]) packet.getPayload());
            case GET_ONLINE -> handleGetOnline();
            case GET_HISTORY -> handleGetHistory((String[]) packet.getPayload());
            case SEARCH_MESSAGES -> handleSearch((String[]) packet.getPayload());
            default -> System.out.println("️ Неизвестная команда: " + packet.getType());
        }
    }

    // === РЕГИСТРАЦИЯ ===
    private void handleRegister(String[] credentials) {
        String nick = credentials[0].trim().toLowerCase();
        String password = credentials[1];

        if (!nick.matches("^[a-z].*")) {
            sendAuthFail("Ник должен начинаться с буквы"); return;
        }
        if (nick.length() < 3) {
            sendAuthFail("Ник слишком короткий (мин. 3 символа)"); return;
        }
        if (userRepository.existsByNickIgnoreCase(nick)) {
            sendAuthFail("Пользователь уже существует"); return;
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        userRepository.save(new User(nick, hashed));

        currentNick = nick;
        sessionRegistry.addSession(nick, this); // <-- Добавляем в онлайн
        sendAuthSuccess(nick);
    }

    // === ВХОД ===
    private void handleLogin(String[] credentials) {
        String nick = credentials[0].trim().toLowerCase();
        String password = credentials[1];

        Optional<User> userOpt = userRepository.findByNickIgnoreCase(nick);
        if (userOpt.isEmpty() || !BCrypt.checkpw(password, userOpt.get().getPasswordHash())) {
            sendAuthFail("Неверный логин или пароль"); return;
        }

        currentNick = nick;
        sessionRegistry.addSession(nick, this); // <-- Добавляем в онлайн
        sendAuthSuccess(nick);
    }

    // === ОТПРАВКА СООБЩЕНИЯ ===
    private void handleSendMessage(String[] data) {
        String sender = data[0];
        String receiverNick = data[1]; // null = всем
        String text = data[2];

        Optional<User> senderOpt = userRepository.findByNickIgnoreCase(sender);
        if (senderOpt.isEmpty()) return;

        User senderUser = senderOpt.get();
        User receiverUser = null;

        // Если личное сообщение, находим получателя в БД
        if (receiverNick != null && !receiverNick.equals("null")) {
            receiverUser = userRepository.findByNickIgnoreCase(receiverNick).orElse(null);
        }

        // Сохраняем в БД
        Message msg = new Message(senderUser, receiverUser, text);
        messageRepository.save(msg);
        System.out.println("💾 [DB] Сообщение ID=" + msg.getId() + " от " + sender +
                (receiverNick != null ? " к " + receiverNick : " всем"));

        // Формируем пакет для пересылки
        NetworkPacket msgPacket = new NetworkPacket(CommandType.RECEIVE_MESSAGE,
                new String[]{sender, receiverNick, text});

        // Маршрутизация
        if (receiverNick == null || receiverNick.equals("null")) {
            // Рассылка всем (broadcast)
            sessionRegistry.broadcastOnlineUpdate(); // Обновляем список, заодно можно отправить сообщение
            for (ClientHandler handler : sessionRegistry.sessions.values()) {
                handler.sendPacket(msgPacket);
            }
        } else {
            // Личное сообщение
            // === Блок отправки личного сообщения ===
            ClientHandler target = sessionRegistry.getSession(receiverNick);
            if (target != null) {
                target.sendPacket(msgPacket);

                // === ОБНОВЛЯЕМ СТАТУС НА DELIVERED ===
                msg.setStatus(MessageStatus.DELIVERED);
                messageRepository.save(msg);
                System.out.println("📬 Статус: DELIVERED для сообщения ID=" + msg.getId());
            } else {
                System.out.println("⚠️ Получатель " + receiverNick + " не в сети (статус: SENT)");
            }
        }
    }

    // === ЗАПРОС СПИСКА ОНЛАЙН ===
    private void handleGetOnline() {
        // Просто триггерим рассылку актуального списка
        sessionRegistry.broadcastOnlineUpdate();
    }

    // === ЗАГРУЗКА ИСТОРИИ (требование №5) ===
    private void handleGetHistory(String[] data) {
        String myNick = data[0];
        String targetNick = data[1];

        System.out.println("📜 [HISTORY] Запрос: " + myNick + " ↔ " + targetNick);

        try {
            var meOpt = userRepository.findByNickIgnoreCase(myNick);
            var targetOpt = userRepository.findByNickIgnoreCase(targetNick);

            if (meOpt.isEmpty()) {
                System.out.println("❌ [HISTORY] Отправитель не найден в БД: " + myNick);
                return;
            }
            if (targetOpt.isEmpty()) {
                System.out.println(" [HISTORY] Получатель не найден в БД: " + targetNick);
                return;
            }

            System.out.println(" [HISTORY] Выполняю JPQL-запрос к БД...");
            List<Message> messages = messageRepository.findLastMessagesBetween(meOpt.get(), targetOpt.get());
            System.out.println("✅ [HISTORY] Найдено сообщений в БД: " + messages.size());

            List<MessageDTO> dtoList = new ArrayList<>();
            int limit = Math.min(messages.size(), 50);
            for (int i = 0; i < limit; i++) {
                Message m = messages.get(i);
                dtoList.add(new MessageDTO(
                        m.getSender().getNick(),
                        m.getReceiver() != null ? m.getReceiver().getNick() : null,
                        m.getText(),
                        m.getSentAt()
                ));
            }

            // Разворачиваем, чтобы в UI шли от старых к новым
            Collections.reverse(dtoList);

            System.out.println("📤 [HISTORY] Отправляю " + dtoList.size() + " сообщений клиенту...");

            // === ОБНОВЛЯЕМ СТАТУС НА READ ПРИ ПРОСМОТРЕ ===
            if (!messages.isEmpty()) {
                for (Message m : messages) {
                    // Меняем статус, только если текущий пользователь — получатель
                    if (m.getReceiver() != null && m.getReceiver().getNick().equals(myNick)) {
                        if (m.getStatus() != MessageStatus.READ) {
                            m.setStatus(MessageStatus.READ);
                        }
                    }
                }
                messageRepository.saveAll(messages);
                System.out.println("👁️ Статус: READ для " + messages.size() + " сообщений");
            }

            sendPacket(new NetworkPacket(CommandType.HISTORY_RESPONSE, dtoList));

        } catch (Exception e) {
            System.err.println("❌ [HISTORY] Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // === ПОИСК ПО СЛОВУ (требование №10) ===
    private void handleSearch(String[] data) {
        String myNick = data[0];
        String targetNick = data[1];
        String keyword = data[2].toLowerCase();

        var meOpt = userRepository.findByNickIgnoreCase(myNick);
        var targetOpt = userRepository.findByNickIgnoreCase(targetNick);

        if (meOpt.isEmpty() || targetOpt.isEmpty()) return;

        // Ищем сообщения, содержащие ключевое слово
        List<Message> found = messageRepository.searchInChat(meOpt.get(), targetOpt.get(), keyword);
        List<MessageDTO> dtoList = new ArrayList<>();

        for (Message m : found) {
            dtoList.add(new MessageDTO(
                    m.getSender().getNick(),
                    m.getReceiver() != null ? m.getReceiver().getNick() : null,
                    m.getText(),
                    m.getSentAt()
            ));
        }

        // === ОБНОВЛЯЕМ СТАТУС НА READ ПРИ ПРОСМОТРЕ РЕЗУЛЬТАТОВ ПОИСКА ===
        if (!found.isEmpty()) {  // <-- ИСПРАВЛЕНО: found, а не messages
            for (Message m : found) {  // <-- ИСПРАВЛЕНО
                // Меняем статус, только если текущий пользователь — получатель
                if (m.getReceiver() != null && m.getReceiver().getNick().equals(myNick)) {
                    if (m.getStatus() != MessageStatus.READ) {
                        m.setStatus(MessageStatus.READ);
                    }
                }
            }
            messageRepository.saveAll(found);  // <-- ИСПРАВЛЕНО
            System.out.println("👁️ Статус: READ для " + found.size() + " сообщений");
        }

        sendPacket(new NetworkPacket(CommandType.SEARCH_RESPONSE, dtoList));
    }

    private void sendAuthSuccess(String nick) {
        sendPacket(new NetworkPacket(CommandType.AUTH_SUCCESS, nick));
    }
    private void sendAuthFail(String msg) {
        sendPacket(new NetworkPacket(CommandType.AUTH_FAIL, msg));
    }

    public void sendPacket(NetworkPacket packet) {
        try { out.writeObject(packet); out.flush(); }
        catch (IOException e) { System.err.println("❌ Ошибка отправки: " + e.getMessage()); close(); }
    }

    public void close() {
        connected = false;
        try { if (in != null) in.close(); if (out != null) out.close(); if (socket != null) socket.close(); }
        catch (IOException e) { System.err.println("Ошибка закрытия: " + e.getMessage()); }
    }
}