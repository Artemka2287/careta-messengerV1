package com.careta.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO для передачи сообщений между клиентом и сервером.
 * Реализует Serializable для отправки через ObjectInputStream.
 */
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sender;        // Ник отправителя
    private String receiver;      // Ник получателя (null = всем)
    private String text;          // Текст сообщения
    private LocalDateTime sentAt; // Время отправки

    // Конструктор по умолчанию (нужен для сериализации)
    public MessageDTO() {}

    // Основной конструктор
    public MessageDTO(String sender, String receiver, String text, LocalDateTime sentAt) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
        this.sentAt = sentAt;
    }

    // Геттеры
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getText() { return text; }
    public LocalDateTime getSentAt() { return sentAt; }
}