package com.careta.server;

import jakarta.persistence.*;
import jakarta.persistence.Entity;

// Сущность для таблицы пользователей
@Entity
@Table(name = "users")  // Имя таблицы в БД
public class User {

    @Id  // Первичный ключ
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Автоинкремент ID
    private Long id;

    @Column(unique = true, nullable = false)  // Уникальный ник, не может быть пустым
    private String nick;  // Имя пользователя (храним в нижнем регистре)

    @Column(nullable = false)
    private String passwordHash;  // Хэш пароля (не храним пароль в открытом виде!)

    // Конструктор по умолчанию (нужен для JPA)
    public User() {}

    // Конструктор с параметрами
    public User(String nick, String passwordHash) {
        this.nick = nick.toLowerCase();  // Приводим к нижнему регистру
        this.passwordHash = passwordHash;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNick() { return nick; }
    public void setNick(String nick) { this.nick = nick.toLowerCase(); }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}