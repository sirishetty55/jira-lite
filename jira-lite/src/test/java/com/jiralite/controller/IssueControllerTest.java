package com.jiralite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jiralite.dto.request.AssignIssueRequest;
import com.jiralite.dto.request.CreateIssueRequest;
import com.jiralite.dto.request.UpdateIssueRequest;
import com.jiralite.dto.request.UpdateIssueStatusRequest;
import com.jiralite.dto.response.ApiResponse;
import com.jiralite.dto.response.IssueDetailResponse;
import com.jiralite.dto.response.IssueResponse;
import com.jiralite.dto.response.UserResponse;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.GlobalExceptionHandler;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import com.jiralite.model.Role;
import com.jiralite.service.ActivityLogService;
import com.jiralite.service.IssueService;
import com.jiralite.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IssueService issueService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private IssueController issueController;

    private ObjectMapper objectMapper;
    private User mockUser;
    private IssueResponse mockIssueResponse;

    @BeforeEach
    void setUp() {
        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("alice@test.com")
                .password("password")
                .roles("DEVELOPER")
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

        mockMvc = MockMvcBuilders.standaloneSetup(issueController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(), userDetailsResolver)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockUser = User.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@test.com")
                .role(Role.DEVELOPER)
                .active(true)
                .build();
        mockUser.setId(1L);

        UserResponse authorResponse = new UserResponse(
                1L, "Alice", "Smith", "alice@test.com", Role.DEVELOPER, true, LocalDateTime.now());

        mockIssueResponse = new IssueResponse(
                10L,
                "TEST-10",
                "Implement login page",
                IssueType.TASK,
                IssueStatus.OPEN,
                Priority.HIGH,
                1L,
                "TEST",
                authorResponse,
                null,
                null,
                1L,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/issues/project/{id}: 201 Created on valid input")
    void createIssue_Valid_Returns201() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Implement login page", "Description", IssueType.TASK, Priority.HIGH, null, null);

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.createIssue(eq(1L), any(CreateIssueRequest.class), eq(mockUser)))
                .thenReturn(mockIssueResponse);

        mockMvc.perform(post("/api/issues/project/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issueKey").value("TEST-10"))
                .andExpect(jsonPath("$.data.title").value("Implement login page"));
    }

    @Test
    @DisplayName("POST /api/issues/project/{id}: 400 Bad Request on blank title")
    void createIssue_BlankTitle_Returns400() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "", "Description", IssueType.TASK, Priority.HIGH, null, null);

        mockMvc.perform(post("/api/issues/project/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    @DisplayName("GET /api/issues/{id}: 200 OK when found and accessible")
    void getIssueById_Found_Returns200() throws Exception {
        IssueDetailResponse detailResponse = new IssueDetailResponse(
                10L,
                "TEST-10",
                "Implement login page",
                "Full description text",
                IssueType.TASK,
                IssueStatus.OPEN,
                Priority.HIGH,
                1L,
                "Alpha Project",
                "TEST",
                UserResponse.from(mockUser),
                null,
                null,
                0L,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.getIssueById(eq(10L), eq(mockUser))).thenReturn(detailResponse);

        mockMvc.perform(get("/api/issues/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issueKey").value("TEST-10"))
                .andExpect(jsonPath("$.data.projectName").value("Alpha Project"));
    }

    @Test
    @DisplayName("GET /api/issues/{id}: 404 Not Found when issue doesn't exist")
    void getIssueById_NotFound_Returns404() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.getIssueById(eq(999L), eq(mockUser)))
                .thenThrow(new ResourceNotFoundException("Issue not found with id: 999"));

        mockMvc.perform(get("/api/issues/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Issue not found with id: 999"));
    }

    @Test
    @DisplayName("GET /api/issues/{id}: 403 Forbidden when user is not a project member")
    void getIssueById_Forbidden_Returns403() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.getIssueById(eq(10L), eq(mockUser)))
                .thenThrow(new AccessDeniedException("You do not have access to this project"));

        mockMvc.perform(get("/api/issues/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have access to this project"));
    }

    @Test
    @DisplayName("PATCH /api/issues/{id}/status: 200 OK on valid status transition")
    void updateIssueStatus_Valid_Returns200() throws Exception {
        UpdateIssueStatusRequest request = new UpdateIssueStatusRequest(IssueStatus.RESOLVED);

        IssueResponse updatedResponse = new IssueResponse(
                10L, "TEST-10", "Implement login page", IssueType.TASK, IssueStatus.RESOLVED,
                Priority.HIGH, 1L, "TEST", UserResponse.from(mockUser), null, null, 2L, LocalDateTime.now());

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.updateIssueStatus(eq(10L), any(UpdateIssueStatusRequest.class), eq(mockUser)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/issues/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("PATCH /api/issues/{id}/assign: 400 Bad Request when assignee is not a member")
    void assignIssue_AssigneeNotMember_Returns400() throws Exception {
        AssignIssueRequest request = new AssignIssueRequest(99L);

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.assignIssue(eq(10L), any(AssignIssueRequest.class), eq(mockUser)))
                .thenThrow(new BadRequestException("Assignee must be an active member of this project"));

        mockMvc.perform(patch("/api/issues/10/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Assignee must be an active member of this project"));
    }

    @Test
    @DisplayName("DELETE /api/issues/{id}: 200 OK on successful deletion")
    void deleteIssue_Success_Returns200() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        doNothing().when(issueService).deleteIssue(eq(10L), eq(mockUser));

        mockMvc.perform(delete("/api/issues/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Issue deleted successfully"));

        verify(issueService, times(1)).deleteIssue(10L, mockUser);
    }

    @Test
    @DisplayName("GET /api/issues/project/{id}: 200 OK with paginated list")
    void getIssuesByProject_Returns200() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(issueService.getIssuesByProject(eq(1L), any(), any(), eq(mockUser), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockIssueResponse), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/issues/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].issueKey").value("TEST-10"));
    }
}

