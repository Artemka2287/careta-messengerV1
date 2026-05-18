package com.careta.server;

import com.careta.common.CommandType;
import com.careta.common.NetworkPacket;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище активных сессий (ник → обработчик клиента).
 * Потокобезопасное, используется для рассылки сообщений и обновления списка онлайн.
 */
@Component
public class SessionRegistry {

    // Ник -> ClientHandler (поток чтения/записи)
    public final Map<String, ClientHandler> sessions = new ConcurrentHashMap<>();

    /** Добавить пользователя в онлайн */
    public void addSession(String nick, ClientHandler handler) {
        sessions.put(nick, handler);
        System.out.println("🟢 " + nick + " подключился. Онлайн: " + sessions.keySet());
        broadcastOnlineUpdate();
    }

    /** Удалить пользователя из онлайн */
    public void removeSession(String nick) {
        if (nick != null) {
            sessions.remove(nick);
            System.out.println("🔴 " + nick + " отключился. Онлайн: " + sessions.keySet());
            broadcastOnlineUpdate();
        }
    }

    /** Получить обработчик по нику (для личных сообщений) */
    public ClientHandler getSession(String nick) {
        return sessions.get(nick);
    }

    /** Рассылка обновлённого списка всем подключённым */
    public void broadcastOnlineUpdate() {
        List<String> onlineNicks = new ArrayList<>(sessions.keySet());
        NetworkPacket packet = new NetworkPacket(CommandType.ONLINE_UPDATE, onlineNicks);

        // Отправляем всем активным сессиям
        for (ClientHandler handler : sessions.values()) {
            handler.sendPacket(packet);
        }
    }
}