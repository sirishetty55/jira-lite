package com.jiralite.service;

import com.jiralite.dto.request.AddProjectMemberRequest;
import com.jiralite.dto.request.AssignIssueRequest;
import com.jiralite.dto.request.CreateIssueRequest;
import com.jiralite.dto.request.UpdateIssueRequest;
import com.jiralite.dto.request.UpdateProjectRequest;
import com.jiralite.dto.response.IssueDetailResponse;
import com.jiralite.dto.response.ProjectDetailResponse;
import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.BadRequestException;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import com.jiralite.model.Role;
import com.jiralite.repository.ActivityLogRepository;
import com.jiralite.repository.CommentRepository;
import com.jiralite.repository.IssueRepository;
import com.jiralite.repository.ProjectMemberRepository;
import com.jiralite.repository.ProjectRepository;
import com.jiralite.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies Priority 0 security and authorization requirements:
 * cross-project isolation, role-based access, assignee project membership,
 * and scoped search.
 */
@ExtendWith(MockitoExtension.class)
class SecurityAndAuthorizationTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectService projectService;
    @Mock
    private UserService userService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private NotificationService notificationService;

    private IssueService issueService;
    private ProjectService concreteProjectService;
    private ActivityLogService concreteActivityLogService;
    private CommentService commentService;
    private UserService concreteUserService;

    private User adminUser;
    private User ownerPM;
    private User nonOwnerPM;
    private User devMember;
    private User devNonMember;
    private Project sampleProject;
    private Issue sampleIssue;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().firstName("Admin").lastName("User").email("admin@test.com").role(Role.ADMIN).build();
        adminUser.setId(1L);

        ownerPM = User.builder().firstName("Owner").lastName("PM").email("owner@test.com").role(Role.PROJECT_MANAGER).build();
        ownerPM.setId(2L);

        nonOwnerPM = User.builder().firstName("Guest").lastName("PM").email("guest@test.com").role(Role.PROJECT_MANAGER).build();
        nonOwnerPM.setId(3L);

        devMember = User.builder().firstName("Dev").lastName("Member").email("member@test.com").role(Role.DEVELOPER).build();
        devMember.setId(4L);

        devNonMember = User.builder().firstName("Outsider").lastName("Dev").email("outsider@test.com").role(Role.DEVELOPER).build();
        devNonMember.setId(5L);

        sampleProject = Project.builder()
                .name("Phoenix")
                .projectKey("PHX")
                .owner(ownerPM)
                .build();
        sampleProject.setId(100L);

        sampleIssue = Issue.builder()
                .title("Feature X")
                .issueKey("PHX-1")
                .issueNumber(1L)
                .type(IssueType.TASK)
                .status(IssueStatus.OPEN)
                .priority(Priority.HIGH)
                .project(sampleProject)
                .reporter(ownerPM)
                .build();
        sampleIssue.setId(200L);

        issueService = new IssueService(issueRepository, commentRepository, projectService, userService, activityLogService, notificationService);
        concreteProjectService = new ProjectService(projectRepository, memberRepository, userService, activityLogService, notificationService);
        concreteActivityLogService = new ActivityLogService(activityLogRepository, projectRepository, issueRepository, memberRepository);
        commentService = new CommentService(commentRepository, issueService, projectService, activityLogService, notificationService);
        concreteUserService = new UserService(userRepository);
    }

    // ── 1. Cross-Project Issue Access ─────────────────────────

    @Test
    @DisplayName("getIssueById: non-member user is denied access")
    void getIssueById_NonMember_ThrowsAccessDenied() {
        when(issueRepository.findById(200L)).thenReturn(Optional.of(sampleIssue));
        when(commentRepository.countByIssue(sampleIssue)).thenReturn(0L);
        when(projectService.findProjectById(100L)).thenReturn(sampleProject);
        when(projectService.isProjectMember(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> issueService.getIssueById(200L, devNonMember))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission to view this issue");
    }

    @Test
    @DisplayName("getIssueById: project member is granted access")
    void getIssueById_Member_Succeeds() {
        when(issueRepository.findById(200L)).thenReturn(Optional.of(sampleIssue));
        when(commentRepository.countByIssue(sampleIssue)).thenReturn(2L);
        when(projectService.findProjectById(100L)).thenReturn(sampleProject);
        when(projectService.isProjectMember(sampleProject, devMember)).thenReturn(true);

        IssueDetailResponse response = issueService.getIssueById(200L, devMember);
        assertThat(response).isNotNull();
        assertThat(response.issueKey()).isEqualTo("PHX-1");
    }

    @Test
    @DisplayName("getIssuesByProject: non-member is denied listing issues")
    void getIssuesByProject_NonMember_ThrowsAccessDenied() {
        when(projectService.findProjectById(100L)).thenReturn(sampleProject);
        when(projectService.isProjectMember(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> issueService.getIssuesByProject(100L, null, null, devNonMember, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission to view issues");
    }

    // ── 2. Assignee Project Membership Validation ──────────────

    @Test
    @DisplayName("createIssue: assigning to non-project-member throws BadRequestException")
    void createIssue_AssigneeNotMember_ThrowsBadRequest() {
        CreateIssueRequest request = new CreateIssueRequest("New Bug", "Desc", IssueType.BUG, Priority.HIGH, 5L, null);

        when(projectService.findProjectById(100L)).thenReturn(sampleProject);
        when(projectService.isProjectMember(sampleProject, devMember)).thenReturn(true);
        when(userService.findUserById(5L)).thenReturn(devNonMember);
        when(projectService.isProjectMember(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> issueService.createIssue(100L, request, devMember))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assignee must be a member of the project");
    }

    @Test
    @DisplayName("assignIssue: assigning to non-project-member throws BadRequestException")
    void assignIssue_AssigneeNotMember_ThrowsBadRequest() {
        AssignIssueRequest request = new AssignIssueRequest(5L);

        when(issueRepository.findById(200L)).thenReturn(Optional.of(sampleIssue));
        when(projectService.isProjectMember(sampleProject, devMember)).thenReturn(true);
        when(userService.findUserById(5L)).thenReturn(devNonMember);
        when(projectService.isProjectMember(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> issueService.assignIssue(200L, request, devMember))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assignee must be a member of the project");
    }

    // ── 3. Project Ownership for PMs ──────────────────────────

    @Test
    @DisplayName("updateProject: non-owner PM is denied updating project metadata")
    void updateProject_NonOwnerPM_ThrowsAccessDenied() {
        UpdateProjectRequest request = new UpdateProjectRequest("Renamed", "Desc");
        when(projectRepository.findById(100L)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> concreteProjectService.updateProject(100L, request, nonOwnerPM))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only Admins or the project owner");
    }

    @Test
    @DisplayName("addMember: non-owner PM is denied adding members")
    void addMember_NonOwnerPM_ThrowsAccessDenied() {
        AddProjectMemberRequest request = new AddProjectMemberRequest(4L);
        when(projectRepository.findById(100L)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> concreteProjectService.addMember(100L, request, nonOwnerPM))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only Admins or the project owner");
    }

    @Test
    @DisplayName("getProjectById: non-member is denied accessing project details")
    void getProjectById_NonMember_ThrowsAccessDenied() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(sampleProject));
        when(memberRepository.findByProject(eq(sampleProject), any())).thenReturn(new PageImpl<>(List.of()));
        when(memberRepository.existsByProjectAndUser(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> concreteProjectService.getProjectById(100L, devNonMember))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission to view this project");
    }

    // ── 4. Scoped Search ──────────────────────────────────────

    @Test
    @DisplayName("searchIssues: Developer queries only accessible issues")
    void searchIssues_Developer_UsesAccessibleFilter() {
        when(issueRepository.searchAccessibleIssues(eq(devMember), eq("bug"), any())).thenReturn(new PageImpl<>(List.of(sampleIssue)));

        Page<com.jiralite.dto.response.IssueResponse> result = issueService.searchIssues("bug", devMember, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(issueRepository).searchAccessibleIssues(eq(devMember), eq("bug"), any());
        verify(issueRepository, never()).searchIssues(any(), any());
    }

    @Test
    @DisplayName("searchIssues: Admin queries global issues across all projects")
    void searchIssues_Admin_UsesGlobalSearch() {
        when(issueRepository.searchIssues(eq("bug"), any())).thenReturn(new PageImpl<>(List.of(sampleIssue)));

        Page<com.jiralite.dto.response.IssueResponse> result = issueService.searchIssues("bug", adminUser, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(issueRepository).searchIssues(eq("bug"), any());
        verify(issueRepository, never()).searchAccessibleIssues(any(), any(), any());
    }

    // ── 5. Comments & Audit Authorization ─────────────────────

    @Test
    @DisplayName("getCommentsByIssue: non-member is denied reading issue comments")
    void getCommentsByIssue_NonMember_ThrowsAccessDenied() {
        when(issueRepository.findById(200L)).thenReturn(Optional.of(sampleIssue));
        when(projectService.isProjectMember(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByIssue(200L, devNonMember, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission to view comments");
    }

    @Test
    @DisplayName("getProjectActivity: non-member is denied viewing project audit log")
    void getProjectActivity_NonMember_ThrowsAccessDenied() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(sampleProject));
        when(memberRepository.existsByProjectAndUser(sampleProject, devNonMember)).thenReturn(false);

        assertThatThrownBy(() -> concreteActivityLogService.getProjectActivity(100L, devNonMember, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have permission to view activity");
    }

    // ── 6. Self-Deactivation Prevention ───────────────────────

    @Test
    @DisplayName("deleteUser: user cannot deactivate their own account")
    void deleteUser_SelfDeactivation_ThrowsBadRequest() {
        assertThatThrownBy(() -> concreteUserService.deleteUser(1L, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot deactivate your own account");
    }
}
