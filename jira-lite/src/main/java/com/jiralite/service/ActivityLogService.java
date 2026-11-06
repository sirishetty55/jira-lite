package com.jiralite.service;

import com.jiralite.entity.ActivityLog;
import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.dto.response.ActivityLogResponse;
import com.jiralite.repository.ActivityLogRepository;
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

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
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
    public Page<ActivityLogResponse> getProjectActivity(Long projectId, Pageable pageable) {
        return activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(ActivityLogResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getIssueActivity(Long issueId, Pageable pageable) {
        return activityLogRepository.findByIssueIdOrderByCreatedAtDesc(issueId, pageable)
                .map(ActivityLogResponse::from);
    }
}

