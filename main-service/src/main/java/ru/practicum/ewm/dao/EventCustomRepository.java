package ru.practicum.ewm.dao;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.dto.AdminEventSearchParams;
import ru.practicum.ewm.dto.EventSearchParams;
import ru.practicum.ewm.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EventCustomRepository {

    private final JPAQueryFactory queryFactory;

    public List<Event> findAllPublicEvents(EventSearchParams params,
                                           LocalDateTime rangeStart,
                                           LocalDateTime rangeEnd) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = buildPublicPredicate(params, rangeStart, rangeEnd, event);

        return queryFactory
                .selectFrom(event)
                .where(predicate)
                .orderBy(event.eventDate.asc())
                .fetch();
    }

    public List<Event> findPublicEventsWithPagination(EventSearchParams params,
                                                      LocalDateTime rangeStart,
                                                      LocalDateTime rangeEnd) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = buildPublicPredicate(params, rangeStart, rangeEnd, event);

        return queryFactory
                .selectFrom(event)
                .where(predicate)
                .orderBy(event.eventDate.asc())
                .offset(params.getFrom())
                .limit(params.getSize())
                .fetch();
    }

    public List<Event> findEventsByAdminFilters(AdminEventSearchParams params,
                                                List<Long> users,
                                                List<String> states,
                                                List<Long> categories,
                                                LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd,
                                                boolean usersEmpty,
                                                boolean statesEmpty,
                                                boolean categoriesEmpty) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = buildAdminPredicate(event, users, states, categories,
                rangeStart, rangeEnd, usersEmpty, statesEmpty, categoriesEmpty);

        return queryFactory
                .selectFrom(event)
                .where(predicate)
                .orderBy(event.id.asc())
                .offset(params.getFrom())
                .limit(params.getSize())
                .fetch();
    }

    public List<Event> findUserEventsWithPagination(Long userId, int from, int size) {
        QEvent event = QEvent.event;

        BooleanExpression predicate = event.initiator.id.eq(userId);

        return queryFactory
                .selectFrom(event)
                .where(predicate)
                .orderBy(event.eventDate.desc())
                .offset(from)
                .limit(size)
                .fetch();
    }

    private BooleanExpression buildPublicPredicate(EventSearchParams params,
                                                   LocalDateTime rangeStart,
                                                   LocalDateTime rangeEnd,
                                                   QEvent event) {
        BooleanExpression predicate = event.state.eq(EventState.PUBLISHED);

        // Поиск по тексту
        if (params.getText() != null && !params.getText().isBlank()) {
            String searchPattern = "%" + params.getText().toLowerCase() + "%";
            predicate = predicate.and(
                    event.annotation.toLowerCase().like(searchPattern)
                            .or(event.description.toLowerCase().like(searchPattern))
            );
        }

        // Фильтр по категориям
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(params.getCategories()));
        }

        // Фильтр по платности
        if (params.getPaid() != null) {
            predicate = predicate.and(event.paid.eq(params.getPaid()));
        }

        // Фильтр по диапазону дат
        predicate = predicate.and(event.eventDate.goe(rangeStart));
        if (rangeEnd != null) {
            predicate = predicate.and(event.eventDate.loe(rangeEnd));
        }

        // Фильтр "только доступные" (лимит не превышен)
        if (params.getOnlyAvailable() != null && params.getOnlyAvailable()) {
            predicate = predicate.and(buildOnlyAvailablePredicate(event));
        }

        return predicate;
    }

    private BooleanExpression buildAdminPredicate(QEvent event,
                                                  List<Long> users,
                                                  List<String> states,
                                                  List<Long> categories,
                                                  LocalDateTime rangeStart,
                                                  LocalDateTime rangeEnd,
                                                  boolean usersEmpty,
                                                  boolean statesEmpty,
                                                  boolean categoriesEmpty) {
        BooleanExpression predicate = null;

        // Фильтр по пользователям
        if (!usersEmpty && users != null && !users.isEmpty()) {
            predicate = and(predicate, event.initiator.id.in(users));
        }

        // Фильтр по статусам
        if (!statesEmpty && states != null && !states.isEmpty()) {
            List<EventState> statesList = states.stream()
                    .map(String::trim)
                    .map(EventState::from)
                    .collect(Collectors.toList());
            predicate = and(predicate, event.state.in(statesList));
        }

        // Фильтр по категориям
        if (!categoriesEmpty && categories != null && !categories.isEmpty()) {
            predicate = and(predicate, event.category.id.in(categories));
        }

        // Фильтр по диапазону дат
        predicate = and(predicate, event.eventDate.goe(rangeStart));
        predicate = and(predicate, event.eventDate.loe(rangeEnd));

        // Если нет условий, возвращаем "всегда true"
        return predicate != null ? predicate : event.isNotNull();
    }

    private BooleanExpression buildOnlyAvailablePredicate(QEvent event) {
        QParticipationRequest request = QParticipationRequest.participationRequest;

        return event.participantLimit.eq(0)
                .or(event.participantLimit.gt(
                        queryFactory.select(request.count())
                                .from(request)
                                .where(request.event.eq(event)
                                        .and(request.status.eq(RequestStatus.CONFIRMED)))
                ));
    }

    private BooleanExpression and(BooleanExpression current, BooleanExpression newCondition) {
        if (current == null) {
            return newCondition;
        }
        return current.and(newCondition);
    }
}