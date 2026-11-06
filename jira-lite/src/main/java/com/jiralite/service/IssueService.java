package com.jiralite.service;

import com.jiralite.dto.request.AssignIssueRequest;
import com.jiralite.dto.request.CreateIssueRequest;
import com.jiralite.dto.request.UpdateIssueRequest;
import com.jiralite.dto.request.UpdateIssueStatusRequest;
import com.jiralite.dto.response.IssueDetailResponse;
import com.jiralite.dto.response.IssueResponse;
import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.Role;
import com.jiralite.repository.CommentRepository;
import com.jiralite.repository.IssueRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue lifecycle management: creation, assignment, status transitions, search.
 */
@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final CommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public IssueService(IssueRepository issueRepository,
                        CommentRepository commentRepository,
                        ProjectService projectService,
                        UserService userService,
                        ActivityLogService activityLogService,
                        NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    @CacheEvict(value = {"issues", "dashboard", "projects"}, allEntries = true)
    public IssueResponse createIssue(Long projectId, CreateIssueRequest request, User currentUser) {
        Project project = projectService.findProjectById(projectId);

        if (!projectService.isProjectMember(project, currentUser)) {
            throw new AccessDeniedException("You must be a project member to create issues");
        }

        // Generate issue key
        Long maxNumber = issueRepository.findMaxIssueNumberByProject(project);
        long nextNumber = maxNumber + 1;
        String issueKey = project.getProjectKey() + "-" + nextNumber;

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userService.findUserById(request.assigneeId());
            if (!projectService.isProjectMember(project, assignee)) {
                throw new BadRequestException("Assignee must be a member of the project");
            }
        }

        Issue issue = Issue.builder()
                .title(request.title())
                .description(request.description())
                .issueKey(issueKey)
                .issueNumber(nextNumber)
                .type(request.type())
                .status(IssueStatus.OPEN)
                .priority(request.priority())
                .project(project)
                .reporter(currentUser)
                .assignee(assignee)
                .dueDate(request.dueDate())
                .build();

        issue = issueRepository.save(issue);

        activityLogService.log("ISSUE_CREATED", "ISSUE", issue.getId(),
                "Created issue: " + issueKey, project, issue, currentUser);

        if (assignee != null && !assignee.getId().equals(currentUser.getId())) {
            notificationService.create(assignee, "ISSUE_ASSIGNED",
                    "You were assigned to issue: " + issueKey, issue.getId(), "ISSUE");
        }

        return IssueResponse.from(issue);
    }

    @Transactional(readOnly = true)
    public IssueDetailResponse getIssueById(Long id, User currentUser) {
        IssueDetailResponse response = getCachedIssueDetail(id);
        if (currentUser.getRole() != Role.ADMIN) {
            Project project = projectService.findProjectById(response.projectId());
            if (!projectService.isProjectMember(project, currentUser)) {
                throw new AccessDeniedException("You do not have permission to view this issue");
            }
        }
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "issues", key = "#id")
    public IssueDetailResponse getCachedIssueDetail(Long id) {
        Issue issue = findIssueById(id);
        long commentCount = commentRepository.countByIssue(issue);
        return IssueDetailResponse.from(issue, commentCount);
    }

    @Transactional(readOnly = true)
    public IssueDetailResponse getIssueById(Long id) {
        return getCachedIssueDetail(id);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByProject(Long projectId, IssueStatus status,
                                                   String search, User currentUser, Pageable pageable) {
        Project project = projectService.findProjectById(projectId);
        if (!projectService.isProjectMember(project, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view issues for this project");
        }

        if (status != null) {
            return issueRepository.findByProjectAndStatus(project, status, pageable)
                    .map(IssueResponse::from);
        }

        if (search != null && !search.isBlank()) {
            return issueRepository.searchIssuesByProject(project, search, pageable)
                    .map(IssueResponse::from);
        }

        return issueRepository.findByProject(project, pageable).map(IssueResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByProject(Long projectId, IssueStatus status,
                                                   String search, Pageable pageable) {
        Project project = projectService.findProjectById(projectId);

        if (status != null) {
            return issueRepository.findByProjectAndStatus(project, status, pageable)
                    .map(IssueResponse::from);
        }

        if (search != null && !search.isBlank()) {
            return issueRepository.searchIssuesByProject(project, search, pageable)
                    .map(IssueResponse::from);
        }

        return issueRepository.findByProject(project, pageable).map(IssueResponse::from);
    }

    @Transactional
    @CacheEvict(value = {"issues", "dashboard"}, allEntries = true)
    public IssueResponse updateIssue(Long id, UpdateIssueRequest request, User currentUser) {
        Issue issue = findIssueById(id);
        verifyIssueAccess(issue, currentUser);

        if (request.version() != null && !request.version().equals(issue.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Issue.class, issue.getId());
        }

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userService.findUserById(request.assigneeId());
            if (!projectService.isProjectMember(issue.getProject(), assignee)) {
                throw new BadRequestException("Assignee must be a member of the project");
            }
        }

        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setType(request.type());
        issue.setPriority(request.priority());
        issue.setAssignee(assignee);
        issue.setDueDate(request.dueDate());

        issue = issueRepository.save(issue);

        activityLogService.log("ISSUE_UPDATED", "ISSUE", issue.getId(),
                "Updated issue: " + issue.getIssueKey(), issue.getProject(), issue, currentUser);

        return IssueResponse.from(issue);
    }

    @Transactional
    @CacheEvict(value = {"issues", "dashboard"}, allEntries = true)
    public IssueResponse updateIssueStatus(Long id, UpdateIssueStatusRequest request, User currentUser) {
        Issue issue = findIssueById(id);
        verifyIssueAccess(issue, currentUser);

        IssueStatus oldStatus = issue.getStatus();
        issue.setStatus(request.status());
        issue = issueRepository.save(issue);

        activityLogService.log("STATUS_CHANGED", "ISSUE", issue.getId(),
                String.format("Status changed: %s → %s", oldStatus, request.status()),
                issue.getProject(), issue, currentUser);

        // Notify reporter on resolution
        if (request.status() == IssueStatus.RESOLVED
                && !issue.getReporter().getId().equals(currentUser.getId())) {
            notificationService.create(issue.getReporter(), "ISSUE_RESOLVED",
                    "Issue resolved: " + issue.getIssueKey(), issue.getId(), "ISSUE");
        }

        return IssueResponse.from(issue);
    }

    @Transactional
    @CacheEvict(value = {"issues", "dashboard"}, allEntries = true)
    public IssueResponse assignIssue(Long id, AssignIssueRequest request, User currentUser) {
        Issue issue = findIssueById(id);
        verifyIssueAccess(issue, currentUser);

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userService.findUserById(request.assigneeId());
            if (!projectService.isProjectMember(issue.getProject(), assignee)) {
                throw new BadRequestException("Assignee must be a member of the project");
            }

            if (!assignee.getId().equals(currentUser.getId())) {
                notificationService.create(assignee, "ISSUE_ASSIGNED",
                        "You were assigned to issue: " + issue.getIssueKey(), issue.getId(), "ISSUE");
            }
        }

        issue.setAssignee(assignee);
        issue = issueRepository.save(issue);

        String detail = assignee != null
                ? "Assigned to " + assignee.getEmail()
                : "Unassigned";
        activityLogService.log("ISSUE_ASSIGNED", "ISSUE", issue.getId(),
                detail, issue.getProject(), issue, currentUser);

        return IssueResponse.from(issue);
    }

    @Transactional
    @CacheEvict(value = {"issues", "dashboard", "projects"}, allEntries = true)
    public void deleteIssue(Long id, User currentUser) {
        Issue issue = findIssueById(id);
        verifyIssueAccess(issue, currentUser);
        issueRepository.delete(issue);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> searchIssues(String query, User currentUser, Pageable pageable) {
        if (currentUser.getRole() == Role.ADMIN) {
            return issueRepository.searchIssues(query, pageable).map(IssueResponse::from);
        }
        return issueRepository.searchAccessibleIssues(currentUser, query, pageable).map(IssueResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> searchIssues(String query, Pageable pageable) {
        return issueRepository.searchIssues(query, pageable).map(IssueResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IssueResponse> getMyIssues(User currentUser, Pageable pageable) {
        return issueRepository.findByAssignee(currentUser, pageable).map(IssueResponse::from);
    }

    // ── Helpers ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Issue findIssueById(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", id));
    }

    private void verifyIssueAccess(Issue issue, User user) {
        if (user.getRole() == com.jiralite.model.Role.ADMIN) return;
        Project project = issue.getProject();
        if (!projectService.isProjectMember(project, user)) {
            throw new AccessDeniedException("You do not have permission to modify this issue");
        }
    }
}

