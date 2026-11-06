package com.jiralite.dto.response;

import com.jiralite.entity.Project;
import com.jiralite.entity.ProjectMember;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full project detail projection including members.
 */
public record ProjectDetailResponse(
        Long id,
        String name,
        String description,
        String projectKey,
        UserResponse owner,
        List<ProjectMemberResponse> members,
        long issueCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectDetailResponse from(Project project, List<ProjectMember> members, long issueCount) {
        return new ProjectDetailResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getProjectKey(),
                UserResponse.from(project.getOwner()),
                members.stream().map(ProjectMemberResponse::from).toList(),
                issueCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}

