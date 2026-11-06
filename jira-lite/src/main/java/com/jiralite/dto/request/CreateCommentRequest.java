package com.jiralite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a comment.
 */
public record CreateCommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 5000, message = "Comment must not exceed 5000 characters")
        String content
) {}

