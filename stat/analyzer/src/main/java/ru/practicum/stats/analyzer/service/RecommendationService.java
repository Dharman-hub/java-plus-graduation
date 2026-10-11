package ru.practicum.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.model.UserInteraction;
import ru.practicum.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.stats.analyzer.repository.UserInteractionRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final UserInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        Set<Long> interactedEvents = interactionRepository
                .findByUserIdOrderByLastInteractionDesc(userId)
                .stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        return similarityRepository.findByEventAOrEventB(eventId, eventId)
                .stream()
                .filter(similarity -> !interactedEvents.contains(getOtherEventId(similarity, eventId)))
                .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                .limit(maxResults)
                .map(similarity -> createRecommendedEvent(
                        getOtherEventId(similarity, eventId),
                        similarity.getScore()
                ))
                .toList();
    }

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserInteraction> interactions =
                interactionRepository.findByUserIdOrderByLastInteractionDesc(userId);

        if (interactions.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> userWeights = interactions.stream()
                .collect(Collectors.toMap(
                        UserInteraction::getEventId,
                        UserInteraction::getWeight
                ));

        Set<Long> recentEvents = interactions.stream()
                .limit(maxResults)
                .map(UserInteraction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> candidates = new HashMap<>();

        List<EventSimilarity> similarities =
                similarityRepository.findByEventAInOrEventBIn(recentEvents, recentEvents);

        for (EventSimilarity similarity : similarities) {
            long eventA = similarity.getEventA();
            long eventB = similarity.getEventB();

            if (recentEvents.contains(eventA) && !userWeights.containsKey(eventB)) {
                candidates.merge(eventB, similarity.getScore(), Math::max);
            }

            if (recentEvents.contains(eventB) && !userWeights.containsKey(eventA)) {
                candidates.merge(eventA, similarity.getScore(), Math::max);
            }
        }

        List<Long> recommendedIds = candidates.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        return recommendedIds.stream()
                .map(eventId -> calculatePrediction(eventId, userWeights, maxResults))
                .filter(recommendation -> recommendation != null)
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .toList();
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = eventIds.stream().distinct().toList();

        Map<Long, Double> counts = new HashMap<>();

        for (UserInteraction interaction : interactionRepository.findByEventIdIn(uniqueIds)) {
            counts.merge(interaction.getEventId(), interaction.getWeight(), Double::sum);
        }

        return uniqueIds.stream()
                .map(eventId -> createRecommendedEvent(
                        eventId,
                        counts.getOrDefault(eventId, 0.0)
                ))
                .toList();
    }

    private RecommendedEventProto calculatePrediction(
            long eventId,
            Map<Long, Double> userWeights,
            int maxNeighbors) {

        List<EventSimilarity> neighbors = similarityRepository
                .findByEventAOrEventB(eventId, eventId)
                .stream()
                .filter(similarity -> userWeights.containsKey(getOtherEventId(similarity, eventId)))
                .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                .limit(maxNeighbors)
                .toList();

        double weightedSum = 0;
        double similaritySum = 0;

        for (EventSimilarity similarity : neighbors) {
            long neighborId = getOtherEventId(similarity, eventId);
            double score = similarity.getScore();

            weightedSum += score * userWeights.get(neighborId);
            similaritySum += score;
        }

        if (similaritySum == 0) {
            return null;
        }

        return createRecommendedEvent(eventId, weightedSum / similaritySum);
    }

    private long getOtherEventId(EventSimilarity similarity, long eventId) {
        return similarity.getEventA() == eventId
                ? similarity.getEventB()
                : similarity.getEventA();
    }

    private RecommendedEventProto createRecommendedEvent(long eventId, double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}