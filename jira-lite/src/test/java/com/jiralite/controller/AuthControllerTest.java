package com.jiralite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jiralite.dto.request.LoginRequest;
import com.jiralite.dto.request.RegisterRequest;
import com.jiralite.dto.response.AuthResponse;
import com.jiralite.dto.response.UserResponse;
import com.jiralite.exception.GlobalExceptionHandler;
import com.jiralite.model.Role;
import com.jiralite.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/auth/register: returns 201 on valid input")
    void register_ValidInput_Returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice", "Smith", "alice@example.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "Alice", "Smith", "alice@example.com", Role.DEVELOPER, true, LocalDateTime.now());
        AuthResponse authResponse = new AuthResponse("mocked_jwt_token", userResponse);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mocked_jwt_token"))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login: returns 200 on success")
    void login_ValidCredentials_Returns200() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "Alice", "Smith", "alice@example.com", Role.DEVELOPER, true, LocalDateTime.now());
        AuthResponse authResponse = new AuthResponse("mocked_jwt_token", userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mocked_jwt_token"));
    }
}

