package com.jiralite.dto.response;

import com.jiralite.entity.ActivityLog;

import java.time.LocalDateTime;

/**
 * Activity log projection.
 */
public record ActivityLogResponse(
        Long id,
        String action,
        String entityType,
        Long entityId,
        String details,
        UserResponse user,
        LocalDateTime createdAt
) {
    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                UserResponse.from(log.getUser()),
                log.getCreatedAt()
        );
    }
}

