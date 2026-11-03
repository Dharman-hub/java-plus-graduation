package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.analyzer.model.UserInteraction;
import ru.practicum.stats.analyzer.repository.UserInteractionRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserInteractionService {

    private final UserInteractionRepository repository;

    @Transactional
    public void process(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();

        double weight = switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        UserInteraction interaction = repository
                .findByUserIdAndEventId(userId, eventId)
                .orElseGet(UserInteraction::new);

        interaction.setUserId(userId);
        interaction.setEventId(eventId);

        if (interaction.getWeight() == null || weight > interaction.getWeight()) {
            interaction.setWeight(weight);
        }

        Instant timestamp = action.getTimestamp();

        if (interaction.getLastInteraction() == null
                || timestamp.isAfter(interaction.getLastInteraction())) {
            interaction.setLastInteraction(timestamp);
        }

        repository.save(interaction);
    }
}