package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.request.EventClient;
import ru.practicum.ewm.client.request.UserClient;
import ru.practicum.ewm.dao.ParticipationRequestRepository;
import ru.practicum.ewm.dto.EventRequestInfoDto;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private static final Logger log = LoggerFactory.getLogger(ParticipationRequestServiceImpl.class);

    private final ParticipationRequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(long ownerId, long eventId) {
        getEventIfExistWithOwnerValidation(eventId, ownerId);

        return requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateOwnParticipationRequests(
            Long ownerId,
            Long eventId,
            EventRequestStatusUpdateRequest request) {

        EventRequestInfoDto event = getEventIfExistWithOwnerValidation(eventId, ownerId);

        long limit = event.getParticipantLimit();

        if (limit == 0) {
            throw new ConflictException("The participant limit is 0, moderation is disabled");
        }

        if (!event.getRequestModeration()) {
            throw new ConflictException("The request moderation is disabled");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);

        long countConfirmed = requests.stream()
                .filter(req -> req.getStatus() == RequestStatus.CONFIRMED)
                .count();

        if (countConfirmed >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        Set<Long> idsToCheck = new HashSet<>(request.getRequestIds());

        boolean allPending = requests.stream()
                .filter(req -> idsToCheck.contains(req.getId()))
                .allMatch(req -> req.getStatus() == RequestStatus.PENDING);

        if (!allPending) {
            throw new ValidationException("Request must have status PENDING");
        }

        List<ParticipationRequest> updatedRequests = new ArrayList<>();
        List<ParticipationRequest> toConfirm = new ArrayList<>();
        List<ParticipationRequest> toReject;

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            long availableSlots = limit - countConfirmed;

            List<ParticipationRequest> targetRequests = requests.stream()
                    .filter(req -> request.getRequestIds().contains(req.getId()))
                    .toList();

            toConfirm = targetRequests.stream()
                    .limit(availableSlots)
                    .toList();

            toReject = targetRequests.stream()
                    .skip(availableSlots)
                    .toList();

            toConfirm.forEach(req -> req.setStatus(RequestStatus.CONFIRMED));
            toReject.forEach(req -> req.setStatus(RequestStatus.REJECTED));
        } else if (request.getStatus() == RequestStatus.REJECTED) {
            toReject = requests.stream()
                    .filter(req -> request.getRequestIds().contains(req.getId()))
                    .toList();

            toReject.forEach(req -> req.setStatus(RequestStatus.REJECTED));
        } else {
            throw new ValidationException(
                    "Field: status. Error: must be CONFIRMED or REJECTED. Value: "
                            + request.getStatus()
            );
        }

        updatedRequests.addAll(toConfirm);
        updatedRequests.addAll(toReject);

        if (!toConfirm.isEmpty()) {
            eventClient.changeConfirmedRequests(eventId, toConfirm.size());
        }

        for (ParticipationRequest req : updatedRequests) {
            log.info(
                    "Save to RequestRepository updated entity: [EventId:{}, RequesterId:{}, Status:{}, Created:{}]",
                    req.getEventId(),
                    req.getRequesterId(),
                    req.getStatus(),
                    req.getCreated()
            );
        }

        requestRepository.saveAll(updatedRequests);

        List<ParticipationRequestDto> confirmed = toConfirm.stream()
                .map(RequestMapper::toDto)
                .toList();

        List<ParticipationRequestDto> rejected = toReject.stream()
                .map(RequestMapper::toDto)
                .toList();

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUserId(Long userId) {
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto sendRequest(Long userId, Long eventId) {
        EventRequestInfoDto event = eventClient.getEvent(eventId);

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException(
                    "Field: eventId. Error: event не найден. Value: " + eventId
            );
        }

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("The request to own event is rejected");
        }

        int limit = event.getParticipantLimit();
        int confirmedRequests = event.getConfirmedRequests();

        if (limit == confirmedRequests && limit != 0) {
            throw new ConflictException(
                    "The participant limit has been reached to event id: " + eventId
            );
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException(
                    "The request to event id: " + eventId
                            + " from user id: " + userId
                            + " is already exists."
            );
        }

        userClient.getUser(userId);

        ParticipationRequest participationRequest = new ParticipationRequest();
        participationRequest.setEventId(eventId);
        participationRequest.setRequesterId(userId);

        if (!event.getRequestModeration() || limit == 0) {
            participationRequest.setStatus(RequestStatus.CONFIRMED);
            eventClient.changeConfirmedRequests(eventId, 1);
        }

        log.info(
                "Save to RequestRepository entity: [EventId:{}, RequesterId:{}, Status:{}, Created:{}]",
                participationRequest.getEventId(),
                participationRequest.getRequesterId(),
                participationRequest.getStatus(),
                participationRequest.getCreated()
        );

        participationRequest = requestRepository.save(participationRequest);

        log.info("request saved with id: {}", participationRequest.getId());

        return RequestMapper.toDto(participationRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new NotFoundException("Request with id= " + requestId + " was not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new NotFoundException(
                    "Request with id= " + requestId + " was not found"
            );
        }

        boolean wasConfirmed = request.getStatus() == RequestStatus.CONFIRMED;

        log.info("Canceling from RequestRepository request: {}", request);

        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);

        if (wasConfirmed) {
            eventClient.changeConfirmedRequests(request.getEventId(), -1);
        }

        return RequestMapper.toDto(request);
    }

    private EventRequestInfoDto getEventIfExistWithOwnerValidation(long eventId, long userId) {
        EventRequestInfoDto event = eventClient.getEvent(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(
                    "Field: userId. Error: Initiator has another id. Value: " + userId
            );
        }

        return event;
    }
}