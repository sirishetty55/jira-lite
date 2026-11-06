package com.jiralite.dto.response;

import com.jiralite.entity.Issue;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full issue detail projection.
 */
public record IssueDetailResponse(
        Long id,
        String issueKey,
        String title,
        String description,
        IssueType type,
        IssueStatus status,
        Priority priority,
        Long projectId,
        String projectName,
        String projectKey,
        UserResponse reporter,
        UserResponse assignee,
        LocalDate dueDate,
        long commentCount,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IssueDetailResponse from(Issue issue, long commentCount) {
        return new IssueDetailResponse(
                issue.getId(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getType(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getProject().getId(),
                issue.getProject().getName(),
                issue.getProject().getProjectKey(),
                UserResponse.from(issue.getReporter()),
                UserResponse.from(issue.getAssignee()),
                issue.getDueDate(),
                commentCount,
                issue.getVersion(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}

