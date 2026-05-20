package com.careta.common;

import java.io.Serializable;

// Универсальный пакет для передачи данных по сети
// Реализует Serializable, чтобы объекты можно было отправлять через ObjectInputStream/ObjectOutputStream
public class NetworkPacket implements Serializable {

    // Уникальный идентификатор версии класса (нужен для стабильной сериализации)
    private static final long serialVersionUID = 1L;

    private CommandType type; // Тип команды (из enum выше)
    private Object payload;   // Данные, привязанные к команде (например, текст сообщения, список пользователей и т.д.)

    // Конструктор без параметров (нужен для сериализации)
    public NetworkPacket() {}

    // Конструктор для быстрой отправки
    public NetworkPacket(CommandType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // Геттеры и сеттеры
    public CommandType getType() { return type; }
    public void setType(CommandType type) { this.type = type; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}