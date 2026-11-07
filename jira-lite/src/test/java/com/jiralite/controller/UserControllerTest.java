package com.jiralite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jiralite.dto.response.UserResponse;
import com.jiralite.entity.User;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.GlobalExceptionHandler;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.Role;
import com.jiralite.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private User mockAdmin;
    private UserResponse adminResponse;
    private UserResponse devResponse;

    @BeforeEach
    void setUp() {
        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@test.com")
                .password("password")
                .roles("ADMIN")
                .build();

        HandlerMethodArgumentResolver userDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(), userDetailsResolver)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockAdmin = User.builder()
                .firstName("Admin")
                .lastName("Super")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .active(true)
                .build();
        mockAdmin.setId(1L);

        adminResponse = new UserResponse(
                1L, "Admin", "Super", "admin@test.com", Role.ADMIN, true, LocalDateTime.now());
        devResponse = new UserResponse(
                2L, "Developer", "One", "dev@test.com", Role.DEVELOPER, true, LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/users/me: returns currently authenticated user profile")
    void getCurrentUser_Returns200() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockAdmin);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET /api/users/{id}: returns 200 with user data when found")
    void getUserById_Found_Returns200() throws Exception {
        when(userService.getUserById(2L)).thenReturn(devResponse);

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.email").value("dev@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id}: returns 404 Not Found when user does not exist")
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));
    }

    @Test
    @DisplayName("GET /api/users: returns only active users in paginated result")
    void getAllUsers_ReturnsActiveUsers_200() throws Exception {
        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(adminResponse, devResponse), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].active").value(true))
                .andExpect(jsonPath("$.data.content[1].active").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("DELETE /api/users/{id}: successfully deactivates other user")
    void deleteUser_OtherUser_Returns200() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockAdmin);
        doNothing().when(userService).deleteUser(eq(2L), eq(mockAdmin));

        mockMvc.perform(delete("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deactivated successfully"));

        verify(userService, times(1)).deleteUser(2L, mockAdmin);
    }

    @Test
    @DisplayName("DELETE /api/users/{id}: 400 Bad Request on self-deactivation attempt")
    void deleteUser_SelfDeactivation_Returns400() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockAdmin);
        doThrow(new BadRequestException("You cannot deactivate your own account"))
                .when(userService).deleteUser(eq(1L), eq(mockAdmin));

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("You cannot deactivate your own account"));
    }
}

