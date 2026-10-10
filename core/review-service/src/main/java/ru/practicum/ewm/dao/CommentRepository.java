package ru.practicum.ewm.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = "SELECT * FROM comments c " +
            "WHERE c.event_id = :eventId " +
            "ORDER BY c.creation_date ASC " +
            "LIMIT :size OFFSET :from",
            nativeQuery = true)
    List<Comment> findAllByEventId(@Param("eventId") Long eventId, @Param("from") int from, @Param("size") int size);

    @Query(value = "SELECT * FROM comments c " +
            "WHERE (:text IS NULL OR LOWER(c.text) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:eventId IS NULL OR c.event_id = :eventId) " +
            "AND (:authorId IS NULL OR c.author_id = :authorId) " +
            "ORDER BY c.creation_date ASC " +
            "LIMIT :size OFFSET :from",
            nativeQuery = true)
    List<Comment> findCommentsAdmin(@Param("text") String text, @Param("eventId") Long eventId,
                                    @Param("authorId") Long authorId, @Param("from") int from,
                                    @Param("size") int size);

    @Query("SELECT c " +
            "FROM Comment c " +
            "WHERE c.parentComment.id = :id")
    List<Comment> findAnswersByCommentId(@Param("id") long commentId);
}