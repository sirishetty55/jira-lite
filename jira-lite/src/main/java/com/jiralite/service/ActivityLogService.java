package com.jiralite.service;

import com.jiralite.entity.ActivityLog;
import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.dto.response.ActivityLogResponse;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.Role;
import com.jiralite.repository.ActivityLogRepository;
import com.jiralite.repository.IssueRepository;
import com.jiralite.repository.ProjectMemberRepository;
import com.jiralite.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and queries audit trail entries for projects and issues.
 */
@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final ProjectMemberRepository memberRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              ProjectRepository projectRepository,
                              IssueRepository issueRepository,
                              ProjectMemberRepository memberRepository) {
        this.activityLogRepository = activityLogRepository;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void log(String action, String entityType, Long entityId,
                    String details, Project project, Issue issue, User user) {
        ActivityLog entry = ActivityLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .project(project)
                .issue(issue)
                .user(user)
                .build();
        activityLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getProjectActivity(Long projectId, User currentUser, Pageable pageable) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (currentUser.getRole() != Role.ADMIN
                && !project.getOwner().getId().equals(currentUser.getId())
                && !memberRepository.existsByProjectAndUser(project, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view activity for this project");
        }
        return activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(ActivityLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getProjectActivity(Long projectId, Pageable pageable) {
        return activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(ActivityLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getIssueActivity(Long issueId, User currentUser, Pageable pageable) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        Project project = issue.getProject();
        if (currentUser.getRole() != Role.ADMIN
                && !project.getOwner().getId().equals(currentUser.getId())
                && !memberRepository.existsByProjectAndUser(project, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view activity for this issue");
        }
        return activityLogRepository.findByIssueIdOrderByCreatedAtDesc(issueId, pageable)
                .map(ActivityLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getIssueActivity(Long issueId, Pageable pageable) {
        return activityLogRepository.findByIssueIdOrderByCreatedAtDesc(issueId, pageable)
                .map(ActivityLogResponse::from);
    }
}

