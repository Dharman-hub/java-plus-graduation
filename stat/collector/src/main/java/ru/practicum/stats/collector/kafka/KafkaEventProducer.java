package ru.practicum.stats.collector.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class KafkaEventProducer {

    private final KafkaProducer<String, UserActionAvro> producer;
    private final String topic;

    public KafkaEventProducer(
            KafkaProducer<String, UserActionAvro> producer,
            @Value("${kafka.topics.user-actions}") String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    public void send(UserActionAvro action) {
        ProducerRecord<String, UserActionAvro> record = new ProducerRecord<>(
                topic,
                action
        );

        producer.send(record);
    }
}