package com.careta.server;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

// Сущность для таблицы сообщений
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne  // Связь Many-to-One: много сообщений у одного отправителя
    @JoinColumn(name = "sender_id")  // Внешний ключ в таблице messages
    private User sender;  // Отправитель

    @ManyToOne  // Связь Many-to-One: много сообщений у одного получателя
    @JoinColumn(name = "receiver_id")  // Внешний ключ
    private User receiver;  // Получатель (null = сообщение всем)

    private String text;  // Текст сообщения

    private LocalDateTime sentAt;  // Дата и время отправки

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT; // Статус доставки (прочитано/не прочитано)

    // Конструктор по умолчанию
    public Message() {}

    // Конструктор для создания сообщения
    public Message(User sender, User receiver, String text) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
        this.sentAt = LocalDateTime.now();  // Текущее время
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
}