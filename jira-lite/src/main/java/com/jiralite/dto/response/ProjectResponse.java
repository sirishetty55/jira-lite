package com.jiralite.dto.response;

import com.jiralite.entity.Project;

import java.time.LocalDateTime;

/**
 * Summary project projection for list views.
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        String projectKey,
        UserResponse owner,
        long memberCount,
        long issueCount,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project, long memberCount, long issueCount) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProjectKey(),
                UserResponse.from(project.getOwner()),
                memberCount,
                issueCount,
                project.getCreatedAt()
        );
    }
}

