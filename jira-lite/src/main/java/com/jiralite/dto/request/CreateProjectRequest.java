package com.jiralite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new project.
 */
public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Project key is required")
        @Size(min = 2, max = 10, message = "Key must be between 2 and 10 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9]*$", message = "Key must start with a letter and contain only uppercase letters/digits")
        String projectKey
) {}

