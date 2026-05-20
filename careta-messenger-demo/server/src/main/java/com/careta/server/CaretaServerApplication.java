package com.careta.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication  // Включает автоконфигурацию Spring, сканирование компонентов и БД
public class CaretaServerApplication {
    public static void main(String[] args) {
        // Запускаем Spring Boot контекст
        SpringApplication.run(CaretaServerApplication.class, args);
    }
}