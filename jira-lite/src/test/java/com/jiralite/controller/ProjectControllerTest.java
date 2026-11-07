package com.jiralite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jiralite.dto.request.AddProjectMemberRequest;
import com.jiralite.dto.request.CreateProjectRequest;
import com.jiralite.dto.request.UpdateProjectRequest;
import com.jiralite.dto.response.ProjectDetailResponse;
import com.jiralite.dto.response.ProjectMemberResponse;
import com.jiralite.dto.response.ProjectResponse;
import com.jiralite.dto.response.UserResponse;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.DuplicateResourceException;
import com.jiralite.exception.GlobalExceptionHandler;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.Role;
import com.jiralite.service.ActivityLogService;
import com.jiralite.service.ProjectService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProjectController projectController;

    private ObjectMapper objectMapper;
    private User mockUser;
    private ProjectResponse mockProjectResponse;

    @BeforeEach
    void setUp() {
        UserDetails mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("pm@test.com")
                .password("password")
                .roles("PROJECT_MANAGER")
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

        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(), userDetailsResolver)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockUser = User.builder()
                .firstName("Project")
                .lastName("Manager")
                .email("pm@test.com")
                .role(Role.PROJECT_MANAGER)
                .active(true)
                .build();
        mockUser.setId(1L);

        UserResponse ownerResponse = new UserResponse(
                1L, "Project", "Manager", "pm@test.com", Role.PROJECT_MANAGER, true, LocalDateTime.now());

        mockProjectResponse = new ProjectResponse(
                10L,
                "Phoenix Project",
                "Cloud Infrastructure",
                "PHX",
                ownerResponse,
                5,
                12,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/projects: 201 Created on valid project creation")
    void createProject_Valid_Returns201() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Phoenix Project", "Cloud Infrastructure", "PHX");

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.createProject(any(CreateProjectRequest.class), eq(mockUser)))
                .thenReturn(mockProjectResponse);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectKey").value("PHX"))
                .andExpect(jsonPath("$.data.name").value("Phoenix Project"));
    }

    @Test
    @DisplayName("POST /api/projects: 400 Bad Request on invalid project key format")
    void createProject_InvalidKey_Returns400() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Phoenix Project", "Cloud Infrastructure", "invalid_key");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("projectKey"));
    }

    @Test
    @DisplayName("POST /api/projects: 409 Conflict on duplicate project key")
    void createProject_DuplicateKey_Returns409() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Phoenix Project", "Cloud Infrastructure", "PHX");

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.createProject(any(CreateProjectRequest.class), eq(mockUser)))
                .thenThrow(new DuplicateResourceException("Project", "key", "PHX"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Project already exists with key: 'PHX'"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}: 200 OK when found and accessible")
    void getProjectById_Found_Returns200() throws Exception {
        ProjectDetailResponse detailResponse = new ProjectDetailResponse(
                10L,
                "Phoenix Project",
                "Cloud Infrastructure",
                "PHX",
                UserResponse.from(mockUser),
                Collections.emptyList(),
                12L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.getProjectById(eq(10L), eq(mockUser))).thenReturn(detailResponse);

        mockMvc.perform(get("/api/projects/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectKey").value("PHX"))
                .andExpect(jsonPath("$.data.name").value("Phoenix Project"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}: 404 Not Found when project does not exist")
    void getProjectById_NotFound_Returns404() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.getProjectById(eq(999L), eq(mockUser)))
                .thenThrow(new ResourceNotFoundException("Project not found with id: 999"));

        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found with id: 999"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}: 403 Forbidden when user is not a member")
    void getProjectById_Forbidden_Returns403() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.getProjectById(eq(10L), eq(mockUser)))
                .thenThrow(new AccessDeniedException("You do not have permission to view this project"));

        mockMvc.perform(get("/api/projects/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to view this project"));
    }

    @Test
    @DisplayName("PUT /api/projects/{id}: 200 OK on valid update")
    void updateProject_Valid_Returns200() throws Exception {
        UpdateProjectRequest request = new UpdateProjectRequest("Phoenix Updated", "Updated Desc");
        ProjectResponse updated = new ProjectResponse(
                10L, "Phoenix Updated", "Updated Desc", "PHX", UserResponse.from(mockUser), 5, 12, LocalDateTime.now());

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.updateProject(eq(10L), any(UpdateProjectRequest.class), eq(mockUser)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/projects/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Phoenix Updated"));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id}: 200 OK on successful deletion")
    void deleteProject_Success_Returns200() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        doNothing().when(projectService).deleteProject(eq(10L), eq(mockUser));

        mockMvc.perform(delete("/api/projects/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Project deleted successfully"));

        verify(projectService, times(1)).deleteProject(10L, mockUser);
    }

    @Test
    @DisplayName("POST /api/projects/{id}/members: 201 Created on adding valid member")
    void addMember_Success_Returns201() throws Exception {
        AddProjectMemberRequest request = new AddProjectMemberRequest(2L);
        UserResponse addedUser = new UserResponse(
                2L, "Bob", "Builder", "bob@test.com", Role.DEVELOPER, true, LocalDateTime.now());
        ProjectMemberResponse memberResponse = new ProjectMemberResponse(
                100L, addedUser, LocalDateTime.now());

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.addMember(eq(10L), any(AddProjectMemberRequest.class), eq(mockUser)))
                .thenReturn(memberResponse);

        mockMvc.perform(post("/api/projects/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("bob@test.com"));
    }

    @Test
    @DisplayName("POST /api/projects/{id}/members: 409 Conflict if user is already a member")
    void addMember_AlreadyMember_Returns409() throws Exception {
        AddProjectMemberRequest request = new AddProjectMemberRequest(2L);

        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.addMember(eq(10L), any(AddProjectMemberRequest.class), eq(mockUser)))
                .thenThrow(new DuplicateResourceException("Member", "userId", 2L));

        mockMvc.perform(post("/api/projects/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id}/members/{userId}: 400 Bad Request when removing project owner")
    void removeMember_Owner_Returns400() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        doThrow(new BadRequestException("Cannot remove the project owner"))
                .when(projectService).removeMember(eq(10L), eq(1L), eq(mockUser));

        mockMvc.perform(delete("/api/projects/10/members/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot remove the project owner"));
    }

    @Test
    @DisplayName("GET /api/projects: 200 OK with paginated list")
    void getAllProjects_Returns200() throws Exception {
        when(userService.getCurrentUser(any())).thenReturn(mockUser);
        when(projectService.getAllProjects(eq(mockUser), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockProjectResponse), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].projectKey").value("PHX"));
    }
}
