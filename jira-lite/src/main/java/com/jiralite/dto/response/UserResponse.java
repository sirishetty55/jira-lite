package com.jiralite.dto.response;

import com.jiralite.entity.User;
import com.jiralite.model.Role;

import java.time.LocalDateTime;

/**
 * User projection exposed through the API.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}

