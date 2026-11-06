package com.jiralite.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for adding a user to a project.
 */
public record AddProjectMemberRequest(
        @NotNull(message = "User ID is required")
        Long userId
) {}

