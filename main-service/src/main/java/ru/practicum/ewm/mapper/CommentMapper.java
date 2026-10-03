package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.model.Comment;

public class CommentMapper {
    public static CommentDto toCommentDto(Comment comment) {
        Long parentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;

        String displayName = comment.getAuthor().getName();

        if (comment.getAuthor().getId().equals(comment.getEvent().getInitiator().getId())) {
            displayName = comment.getEvent().getTitle();
        }

        return CommentDto.builder()
                .id(comment.getId())
                .answerTo(parentId)
                .name(displayName)
                .text(comment.getText())
                .createdOn(comment.getCreationDate())
                .build();
    }
}