package com.jiralite.dto.response;

import java.util.List;

/**
 * Aggregated dashboard statistics.
 */
public record DashboardResponse(
        long totalProjects,
        long totalIssues,
        long openIssues,
        long inProgressIssues,
        long inReviewIssues,
        long resolvedIssues,
        long closedIssues,
        long myAssignedIssues,
        long overdueIssues,
        List<ProjectSummary> recentProjects,
        List<IssueResponse> recentIssues
) {
    /**
     * Lightweight project summary for the dashboard.
     */
    public record ProjectSummary(
            Long id,
            String name,
            String projectKey,
            long issueCount,
            long openIssueCount
    ) {}
}

