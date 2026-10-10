package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.CommentDtoRequest;

import java.util.List;

public interface CommentService {
    CommentDto addComment(Long userId, Long eventId, CommentDtoRequest requestDto);

    CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentDtoRequest updateDto);

    void deleteComment(Long userId, Long eventId, Long commentId);

    void deleteCommentAdmin(Long commentId);

    List<CommentDto> getCommentsAdmin(String text, Long eventId, Long authorId, int from, int size);

    List<CommentDto> getCommentsPublic(Long eventId, int from, int size);
}