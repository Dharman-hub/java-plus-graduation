package ru.practicum.stats.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StatApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(StatApplication.class, args);
    }
}
