package com.jiralite.dto.response;

/**
 * JWT authentication response.
 */
public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    public AuthResponse(String token, UserResponse user) {
        this(token, "Bearer", user);
    }
}

