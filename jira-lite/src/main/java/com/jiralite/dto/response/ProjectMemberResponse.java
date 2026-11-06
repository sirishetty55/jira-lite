package com.jiralite.dto.response;

import com.jiralite.entity.ProjectMember;

import java.time.LocalDateTime;

/**
 * Project member projection.
 */
public record ProjectMemberResponse(
        Long id,
        UserResponse user,
        LocalDateTime joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember pm) {
        return new ProjectMemberResponse(
                pm.getId(),
                UserResponse.from(pm.getUser()),
                pm.getJoinedAt()
        );
    }
}

