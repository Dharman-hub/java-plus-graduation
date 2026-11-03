package ru.practicum.stats.collector.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.stats.collector.kafka.KafkaEventProducer;

import java.time.Instant;

@Service
public class CollectorService {

    private final KafkaEventProducer kafkaEventProducer;

    public CollectorService(KafkaEventProducer kafkaEventProducer) {
        this.kafkaEventProducer = kafkaEventProducer;
    }

    public void collectUserAction(UserActionProto request) {
        ActionTypeAvro actionType = switch (request.getActionType()) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type");
        };

        Instant timestamp = Instant.ofEpochSecond(
                request.getTimestamp().getSeconds(),
                request.getTimestamp().getNanos()
        );

        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();

        kafkaEventProducer.send(action);
    }
}