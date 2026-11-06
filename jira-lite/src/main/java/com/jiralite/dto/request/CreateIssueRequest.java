package com.jiralite.dto.request;

import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating a new issue.
 */
public record CreateIssueRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Issue type is required")
        IssueType type,

        @NotNull(message = "Priority is required")
        Priority priority,

        Long assigneeId,

        LocalDate dueDate
) {}

