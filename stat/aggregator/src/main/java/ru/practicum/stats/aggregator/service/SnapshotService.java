package ru.practicum.stats.aggregator.service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Service
public class SnapshotService {

    private final KafkaProducer<String, EventSimilarityAvro> producer;
    private final String topic;

    private final Map<Long, Map<Long, Double>> eventWeights = new HashMap<>();
    private final Map<Long, Double> weightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    public SnapshotService(
            KafkaProducer<String, EventSimilarityAvro> producer,
            @Value("${kafka.topics.events-similarity}") String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    public void process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();

        double weight = getWeight(action.getActionType());

        Map<Long, Double> userWeights = eventWeights.computeIfAbsent(
                eventId, id -> new HashMap<>()
        );

        double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (weight <= oldWeight) {
            return;
        }

        userWeights.put(userId, weight);

        double newSum = weightSums.getOrDefault(eventId, 0.0)
                + weight - oldWeight;

        weightSums.put(eventId, newSum);

        for (Map.Entry<Long, Map<Long, Double>> entry : eventWeights.entrySet()) {
            long otherEventId = entry.getKey();

            if (eventId == otherEventId) {
                continue;
            }

            double otherWeight = entry.getValue().getOrDefault(userId, 0.0);

            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            Map<Long, Double> pairSums = minWeightSums.computeIfAbsent(
                    first, id -> new HashMap<>()
            );

            double oldMinSum = pairSums.getOrDefault(second, 0.0);

            double delta = Math.min(weight, otherWeight)
                    - Math.min(oldWeight, otherWeight);

            double newMinSum = oldMinSum + delta;

            if (newMinSum == 0.0) {
                continue;
            }

            pairSums.put(second, newMinSum);

            double similarity = newMinSum / Math.sqrt(
                    newSum * weightSums.get(otherEventId)
            );

            EventSimilarityAvro result = EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(similarity)
                    .setTimestamp(action.getTimestamp())
                    .build();

            producer.send(new ProducerRecord<>(
                    topic,
                    first + "_" + second,
                    result
            ));
        }
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}