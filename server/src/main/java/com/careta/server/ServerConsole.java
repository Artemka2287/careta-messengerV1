package com.careta.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component  // Spring автоматически создаст этот компонент при запуске
public class ServerConsole implements CommandLineRunner {

    // Этот метод выполнится сразу после запуска Spring Boot
    @Override
    public void run(String... args) throws Exception {
        System.out.println("════════════════════════════════════════╗");
        System.out.println("║       🚕 МЕССЕНДЖЕР «КАРЕТА»         ║");
        System.out.println("║          СЕРВЕР ЗАПУЩЕН              ║");
        System.out.println("════════════════════════════════════════╣");
        System.out.println("║ Порт для клиентов: 5001              ║");
        System.out.println("║ База данных: H2 (локально)           ║");
        System.out.println("════════════════════════════════════════╝");

        // Держим процесс сервера запущенным
        // В реальном приложении здесь будет цикл приема соединений
        synchronized (this) {
            wait();
        }
    }
}