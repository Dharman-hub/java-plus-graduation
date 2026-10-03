package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dao.CommentRepository;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.dao.UserRepository;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.CommentDtoRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, CommentDtoRequest requestDto) {
        log.info("Пользователь с id={} создает комментарий к событию с id={}", userId, eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не был найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не было найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не было найдено");
        }

        Comment parentComment = null;

        if (requestDto.getAnswerTo() != null) {
            parentComment = commentRepository.findById(requestDto.getAnswerTo())
                    .orElseThrow(() -> new NotFoundException("Родительский комментарий с id=" + requestDto.getAnswerTo()
                            + " не найден"));

            if (!parentComment.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Комментарий с id=" + requestDto.getAnswerTo() + " для ответа не найден");
            }
        }

        Comment comment = Comment.builder()
                .text(requestDto.getText())
                .author(user)
                .event(event)
                .parentComment(parentComment)
                .creationDate(LocalDateTime.now())
                .build();

        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentDtoRequest updateDto) {
        log.info("Пользователь с id={} обновляет комментарий с id={} для события с id={}", userId, commentId, eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не был найден"));

        if (!Objects.equals(comment.getEvent().getId(), eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId +
                    " не был найден в событии id = " + comment.getEvent().getId());
        }

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Только автор может обновить комментарий.");
        }

        comment.setText(updateDto.getText());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        log.info("Пользователь с id={} удаляет комментарий с id={} для события с id={}", userId, commentId, eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не был найден"));

        if (!Objects.equals(comment.getEvent().getId(), eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId +
                    " не был найден в событии id = " + comment.getEvent().getId());
        }

        if (!comment.getAuthor().getId().equals(userId)) {
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
    public List<CommentDto> getCommentsAdmin(String text, Long eventId, Long authorId, int from, int size) {
        log.info("Получение комментариев администратором по фильтрам");

        return commentRepository.findCommentsAdmin(text, eventId, authorId, from, size).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsPublic(Long eventId, int from, int size) {
        log.info("Публичное получение комментариев для события с id={}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не было найдено");
        }

        return commentRepository.findAllByEventId(eventId, from, size).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    private void clearAnswerLinks(long commentId) {
        List<Comment> answers = commentRepository.findAnswersByCommentId(commentId);

        for (Comment comment : answers) {
            comment.setParentComment(null);
        }

        commentRepository.saveAll(answers);
    }
}