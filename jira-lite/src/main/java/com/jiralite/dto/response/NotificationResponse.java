package com.jiralite.dto.response;

import com.jiralite.entity.Notification;

import java.time.LocalDateTime;

/**
 * Notification projection.
 */
public record NotificationResponse(
        Long id,
        String message,
        String type,
        Long referenceId,
        String referenceType,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getMessage(),
                n.getType(),
                n.getReferenceId(),
                n.getReferenceType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}

