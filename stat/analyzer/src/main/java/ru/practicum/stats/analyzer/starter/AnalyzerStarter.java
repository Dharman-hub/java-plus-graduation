package ru.practicum.stats.analyzer.starter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.service.EventSimilarityService;
import ru.practicum.stats.analyzer.service.UserInteractionService;

import java.time.Duration;
import java.util.List;

@Component
public class AnalyzerStarter implements CommandLineRunner {

    private final KafkaConsumer<String, UserActionAvro> userActionsConsumer;
    private final KafkaConsumer<String, EventSimilarityAvro> eventSimilarityConsumer;
    private final UserInteractionService userInteractionService;
    private final EventSimilarityService eventSimilarityService;
    private final String userActionsTopic;
    private final String eventSimilarityTopic;

    public AnalyzerStarter(
            @Qualifier("userActionsConsumer") KafkaConsumer<String, UserActionAvro> userActionsConsumer,
            @Qualifier("eventSimilarityConsumer") KafkaConsumer<String, EventSimilarityAvro> eventSimilarityConsumer,
            UserInteractionService userInteractionService,
            EventSimilarityService eventSimilarityService,
            @Value("${kafka.topics.user-actions}") String userActionsTopic,
            @Value("${kafka.topics.events-similarity}") String eventSimilarityTopic) {
        this.userActionsConsumer = userActionsConsumer;
        this.eventSimilarityConsumer = eventSimilarityConsumer;
        this.userInteractionService = userInteractionService;
        this.eventSimilarityService = eventSimilarityService;
        this.userActionsTopic = userActionsTopic;
        this.eventSimilarityTopic = eventSimilarityTopic;
    }

    @Override
    public void run(String... args) {
        Thread thread = new Thread(this::consume);
        thread.setDaemon(true);
        thread.start();
    }

    private void consume() {
        userActionsConsumer.subscribe(List.of(userActionsTopic));
        eventSimilarityConsumer.subscribe(List.of(eventSimilarityTopic));

        while (!Thread.currentThread().isInterrupted()) {
            ConsumerRecords<String, UserActionAvro> actions =
                    userActionsConsumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, UserActionAvro> record : actions) {
                userInteractionService.process(record.value());
            }

            ConsumerRecords<String, EventSimilarityAvro> similarities =
                    eventSimilarityConsumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, EventSimilarityAvro> record : similarities) {
                eventSimilarityService.process(record.value());
            }
        }
    }
}