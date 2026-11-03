package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import ru.practicum.stats.client.AnalyzerGrpcClient;
import ru.practicum.stats.client.CollectorGrpcClient;

@SpringBootApplication
@Import({CollectorGrpcClient.class, AnalyzerGrpcClient.class})
@ComponentScan(basePackages = {"ru.practicum.ewm", "ru.practicum.stats"})
@EnableFeignClients(basePackages = "ru.practicum.ewm.client")
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}