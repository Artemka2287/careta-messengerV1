package com.careta.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Интерфейс для работы с таблицей users
@Repository  // Аннотация Spring: этот компонент будет автоматически создан
public interface UserRepository extends JpaRepository<User, Long> {

    // Найти пользователя по нику (без учёта регистра)
    Optional<User> findByNickIgnoreCase(String nick);

    // Проверить, существует ли пользователь с таким ником
    boolean existsByNickIgnoreCase(String nick);
}