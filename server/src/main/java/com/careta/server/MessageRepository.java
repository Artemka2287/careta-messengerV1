package com.careta.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

// Интерфейс для работы с таблицей messages
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Получить последние 50 сообщений между двумя пользователями
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1)) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findLastMessagesBetween(
            @Param("user1") User user1,
            @Param("user2") User user2
    );

    // Поиск сообщений по ключевому слову в чате с конкретным пользователем
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1)) " +
            "AND LOWER(m.text) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY m.sentAt DESC")
    List<Message> searchInChat(
            @Param("user1") User user1,
            @Param("user2") User user2,
            @Param("keyword") String keyword
    );

    // Найти все сообщения, отправленные конкретному пользователю
    List<Message> findByReceiverOrderBySentAtDesc(User receiver);
}