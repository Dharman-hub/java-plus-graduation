package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.EventRequestInfoDto;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.model.Comment;

public class CommentMapper {

    public static CommentDto toCommentDto(Comment comment,
                                          UserDto author,
                                          EventRequestInfoDto event) {

        Long parentId = comment.getParentComment() != null
                ? comment.getParentComment().getId()
                : null;

        String displayName = author.getName();

        if (comment.getAuthorId().equals(event.getInitiatorId())) {
            displayName = event.getTitle();
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