package com.jiralite.dto.request;

/**
 * Payload for assigning or unassigning an issue.
 * A null assigneeId unassigns the issue.
 */
public record AssignIssueRequest(
        Long assigneeId
) {}

