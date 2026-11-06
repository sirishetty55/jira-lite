package com.jiralite.service;

import com.jiralite.dto.request.CreateIssueRequest;
import com.jiralite.dto.request.UpdateIssueStatusRequest;
import com.jiralite.dto.response.IssueResponse;
import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import com.jiralite.model.Role;
import com.jiralite.repository.CommentRepository;
import com.jiralite.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IssueService issueService;

    private User devUser;
    private Project project;
    private Issue sampleIssue;

    @BeforeEach
    void setUp() {
        devUser = User.builder()
                .firstName("Jane")
                .lastName("Dev")
                .email("jane@example.com")
                .role(Role.DEVELOPER)
                .build();
        devUser.setId(3L);

        project = Project.builder()
                .name("Beta")
                .projectKey("BETA")
                .owner(devUser)
                .build();
        project.setId(20L);

        sampleIssue = Issue.builder()
                .title("Fix login bug")
                .description("Null pointer on empty input")
                .issueKey("BETA-1")
                .issueNumber(1L)
                .type(IssueType.BUG)
                .status(IssueStatus.OPEN)
                .priority(Priority.HIGH)
                .project(project)
                .reporter(devUser)
                .build();
        sampleIssue.setId(100L);
    }

    @Test
    @DisplayName("createIssue: generates correct key and defaults to OPEN")
    void createIssue_Success() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Fix login bug", "Null pointer on empty input",
                IssueType.BUG, Priority.HIGH, null, null);

        when(projectService.findProjectById(20L)).thenReturn(project);
        when(projectService.isProjectMember(project, devUser)).thenReturn(true);
        when(issueRepository.findMaxIssueNumberByProject(project)).thenReturn(0L);
        when(issueRepository.save(any(Issue.class))).thenReturn(sampleIssue);

        IssueResponse response = issueService.createIssue(20L, request, devUser);

        assertThat(response).isNotNull();
        assertThat(response.issueKey()).isEqualTo("BETA-1");
        assertThat(response.status()).isEqualTo(IssueStatus.OPEN);
        assertThat(response.priority()).isEqualTo(Priority.HIGH);

        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    @DisplayName("createIssue: throws AccessDeniedException when not a project member")
    void createIssue_NonMember_ThrowsException() {
        CreateIssueRequest request = new CreateIssueRequest(
                "Fix login bug", "Null pointer",
                IssueType.BUG, Priority.HIGH, null, null);

        when(projectService.findProjectById(20L)).thenReturn(project);
        when(projectService.isProjectMember(project, devUser)).thenReturn(false);

        assertThatThrownBy(() -> issueService.createIssue(20L, request, devUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("must be a project member");

        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    @DisplayName("updateIssueStatus: transitions status correctly")
    void updateIssueStatus_Success() {
        UpdateIssueStatusRequest request = new UpdateIssueStatusRequest(IssueStatus.IN_PROGRESS);

        when(issueRepository.findById(100L)).thenReturn(Optional.of(sampleIssue));
        when(projectService.isProjectMember(project, devUser)).thenReturn(true);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResponse response = issueService.updateIssueStatus(100L, request, devUser);

        assertThat(response.status()).isEqualTo(IssueStatus.IN_PROGRESS);
        verify(activityLogService).log(eq("STATUS_CHANGED"), eq("ISSUE"), eq(100L),
                contains("IN_PROGRESS"), eq(project), any(), eq(devUser));
    }

    @Test
    @DisplayName("updateIssue: throws ObjectOptimisticLockingFailureException when version mismatch")
    void updateIssue_VersionMismatch_ThrowsOptimisticLockException() {
        sampleIssue.setVersion(2L);
        when(issueRepository.findById(100L)).thenReturn(Optional.of(sampleIssue));
        when(projectService.isProjectMember(project, devUser)).thenReturn(true);

        com.jiralite.dto.request.UpdateIssueRequest staleRequest = new com.jiralite.dto.request.UpdateIssueRequest(
                "Updated title", "Desc", IssueType.BUG, Priority.HIGH, null, null, 1L);

        assertThatThrownBy(() -> issueService.updateIssue(100L, staleRequest, devUser))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }
}

