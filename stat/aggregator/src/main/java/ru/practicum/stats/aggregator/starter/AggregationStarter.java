package ru.practicum.stats.aggregator.starter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.aggregator.service.SnapshotService;

import java.time.Duration;
import java.util.List;

@Component
public class AggregationStarter implements CommandLineRunner {

    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final SnapshotService snapshotService;
    private final String topic;

    public AggregationStarter(
            KafkaConsumer<String, UserActionAvro> consumer,
            SnapshotService snapshotService,
            @Value("${kafka.topics.user-actions}") String topic) {
        this.consumer = consumer;
        this.snapshotService = snapshotService;
        this.topic = topic;
    }

    @Override
    public void run(String... args) {
        consumer.subscribe(List.of(topic));

        while (true) {
            ConsumerRecords<String, UserActionAvro> records =
                    consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, UserActionAvro> record : records) {
                snapshotService.process(record.value());
            }
        }
    }
}