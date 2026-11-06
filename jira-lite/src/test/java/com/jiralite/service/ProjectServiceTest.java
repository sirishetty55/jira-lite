package com.jiralite.service;

import com.jiralite.dto.request.CreateProjectRequest;
import com.jiralite.dto.response.ProjectResponse;
import com.jiralite.entity.Project;
import com.jiralite.entity.ProjectMember;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.DuplicateResourceException;
import com.jiralite.model.Role;
import com.jiralite.repository.ProjectMemberRepository;
import com.jiralite.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProjectService projectService;

    private User adminUser;
    private User devUser;
    private Project sampleProject;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();
        adminUser.setId(1L);

        devUser = User.builder()
                .firstName("Dev")
                .lastName("User")
                .email("dev@example.com")
                .role(Role.DEVELOPER)
                .build();
        devUser.setId(2L);

        sampleProject = Project.builder()
                .name("Alpha Project")
                .description("First project")
                .projectKey("ALPHA")
                .owner(adminUser)
                .build();
        sampleProject.setId(10L);
    }

    @Test
    @DisplayName("createProject: succeeds when user is Admin")
    void createProject_AsAdmin_Success() {
        CreateProjectRequest request = new CreateProjectRequest("Alpha Project", "First project", "ALPHA");

        when(projectRepository.existsByProjectKey("ALPHA")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);
        when(memberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());

        ProjectResponse response = projectService.createProject(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.projectKey()).isEqualTo("ALPHA");
        assertThat(response.name()).isEqualTo("Alpha Project");

        verify(projectRepository).save(any(Project.class));
        verify(memberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("createProject: throws AccessDeniedException when user is Developer")
    void createProject_AsDeveloper_ThrowsAccessDenied() {
        CreateProjectRequest request = new CreateProjectRequest("Alpha Project", "First project", "ALPHA");

        assertThatThrownBy(() -> projectService.createProject(request, devUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only Admins and Project Managers");

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("createProject: throws DuplicateResourceException when key already exists")
    void createProject_DuplicateKey_ThrowsException() {
        CreateProjectRequest request = new CreateProjectRequest("Alpha Project", "First project", "ALPHA");
        when(projectRepository.existsByProjectKey("ALPHA")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(request, adminUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ALPHA");
    }
}

