package com.jiralite.dto.request;

import com.jiralite.model.IssueStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for transitioning an issue's status.
 */
public record UpdateIssueStatusRequest(
        @NotNull(message = "Status is required")
        IssueStatus status
) {}

