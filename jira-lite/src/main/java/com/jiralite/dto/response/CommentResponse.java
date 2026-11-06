package com.jiralite.dto.response;

import com.jiralite.entity.Comment;

import java.time.LocalDateTime;

/**
 * Comment projection.
 */
public record CommentResponse(
        Long id,
        String content,
        UserResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                UserResponse.from(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}

