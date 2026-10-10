package ru.practicum.ewm.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.review.EventClient;
import ru.practicum.ewm.client.review.UserClient;
import ru.practicum.ewm.dao.CommentRepository;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.CommentDtoRequest;
import ru.practicum.ewm.dto.EventRequestInfoDto;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, CommentDtoRequest requestDto) {
        log.info("Пользователь с id={} создает комментарий к событию с id={}", userId, eventId);

        UserDto user = userClient.getUser(userId);
        EventRequestInfoDto event = eventClient.getEvent(eventId);

        if (!"PUBLISHED".equals(event.getState())) {
            throw new NotFoundException("Событие с id=" + eventId + " не было найдено");
        }

        Comment parentComment = null;

        if (requestDto.getAnswerTo() != null) {
            parentComment = commentRepository.findById(requestDto.getAnswerTo())
                    .orElseThrow(() -> new NotFoundException(
                            "Родительский комментарий с id=" + requestDto.getAnswerTo() + " не найден"
                    ));

            if (!parentComment.getEventId().equals(eventId)) {
                throw new NotFoundException(
                        "Комментарий с id=" + requestDto.getAnswerTo() + " для ответа не найден"
                );
            }
        }

        Comment comment = Comment.builder()
                .text(requestDto.getText())
                .authorId(userId)
                .eventId(eventId)
                .parentComment(parentComment)
                .creationDate(LocalDateTime.now())
                .build();

        comment = commentRepository.save(comment);

        return CommentMapper.toCommentDto(comment, user, event);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId,
                                    Long eventId,
                                    Long commentId,
                                    CommentDtoRequest updateDto) {
        log.info(
                "Пользователь с id={} обновляет комментарий с id={} для события с id={}",
                userId,
                commentId,
                eventId
        );

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new NotFoundException("Комментарий с id=" + commentId + " не был найден"));

        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new NotFoundException(
                    "Комментарий с id=" + commentId
                            + " не был найден в событии id = " + comment.getEventId()
            );
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор может обновить комментарий.");
        }

        comment.setText(updateDto.getText());
        comment = commentRepository.save(comment);

        UserDto author = userClient.getUser(comment.getAuthorId());
        EventRequestInfoDto event = eventClient.getEvent(comment.getEventId());

        return CommentMapper.toCommentDto(comment, author, event);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        log.info(
                "Пользователь с id={} удаляет комментарий с id={} для события с id={}",
                userId,
                commentId,
                eventId
        );

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new NotFoundException("Комментарий с id=" + commentId + " не был найден"));

        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new NotFoundException(
                    "Комментарий с id=" + commentId
                            + " не был найден в событии id = " + comment.getEventId()
            );
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Только автор комментария может его удалить.");
        }

        clearAnswerLinks(commentId);
        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public void deleteCommentAdmin(Long commentId) {
        log.info("Администратор удаляет комментарий с id={}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не был найден");
        }

        clearAnswerLinks(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getCommentsAdmin(String text,
                                             Long eventId,
                                             Long authorId,
                                             int from,
                                             int size) {
        log.info("Получение комментариев администратором по фильтрам");

        List<Comment> comments =
                commentRepository.findCommentsAdmin(text, eventId, authorId, from, size);

        return mapComments(comments);
    }

    @Override
    public List<CommentDto> getCommentsPublic(Long eventId, int from, int size) {
        log.info("Публичное получение комментариев для события с id={}", eventId);

        try {
            eventClient.getEvent(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        List<Comment> comments = commentRepository.findAllByEventId(eventId, from, size);

        return mapComments(comments);
    }

    private List<CommentDto> mapComments(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserDto> authors = getAuthors(comments);
        Map<Long, EventRequestInfoDto> events = getEvents(comments);

        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(
                        comment,
                        authors.get(comment.getAuthorId()),
                        events.get(comment.getEventId())
                ))
                .toList();
    }

    private Map<Long, UserDto> getAuthors(Collection<Comment> comments) {
        List<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userClient.getUsers(authorIds).stream()
                .collect(Collectors.toMap(UserDto::getId, user -> user));
    }

    private Map<Long, EventRequestInfoDto> getEvents(Collection<Comment> comments) {
        List<Long> eventIds = comments.stream()
                .map(Comment::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return eventClient.getEvents(eventIds).stream()
                .collect(Collectors.toMap(EventRequestInfoDto::getId, event -> event));
    }

    private void clearAnswerLinks(long commentId) {
        List<Comment> answers = commentRepository.findAnswersByCommentId(commentId);

        for (Comment comment : answers) {
            comment.setParentComment(null);
        }

        commentRepository.saveAll(answers);
    }
}