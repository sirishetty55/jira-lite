package com.jiralite.dto.response;

import com.jiralite.entity.Issue;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Issue summary projection for list views.
 */
public record IssueResponse(
        Long id,
        String issueKey,
        String title,
        IssueType type,
        IssueStatus status,
        Priority priority,
        Long projectId,
        String projectKey,
        UserResponse reporter,
        UserResponse assignee,
        LocalDate dueDate,
        Long version,
        LocalDateTime createdAt
) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getType(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getProject().getId(),
                issue.getProject().getProjectKey(),
                UserResponse.from(issue.getReporter()),
                UserResponse.from(issue.getAssignee()),
                issue.getDueDate(),
                issue.getVersion(),
                issue.getCreatedAt()
        );
    }
}

