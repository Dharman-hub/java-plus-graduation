package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.repository.EventSimilarityRepository;

@Service
@RequiredArgsConstructor
public class EventSimilarityService {

    private final EventSimilarityRepository repository;

    @Transactional
    public void process(EventSimilarityAvro event) {
        long eventA = event.getEventA();
        long eventB = event.getEventB();

        EventSimilarity similarity = repository
                .findByEventAAndEventB(eventA, eventB)
                .orElseGet(EventSimilarity::new);

        similarity.setEventA(eventA);
        similarity.setEventB(eventB);
        similarity.setScore(event.getScore());

        repository.save(similarity);
    }
}