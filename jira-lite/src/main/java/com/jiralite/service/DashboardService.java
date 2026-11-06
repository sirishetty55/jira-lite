package com.jiralite.service;

import com.jiralite.dto.response.DashboardResponse;
import com.jiralite.dto.response.IssueResponse;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.Role;
import com.jiralite.repository.IssueRepository;
import com.jiralite.repository.ProjectRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated statistics for the user dashboard.
 */
@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;

    public DashboardService(ProjectRepository projectRepository,
                            IssueRepository issueRepository) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "#currentUser.id")
    public DashboardResponse getDashboard(User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        long totalProjects = isAdmin
                ? projectRepository.count()
                : projectRepository.findProjectsByMember(currentUser, PageRequest.of(0, 1)).getTotalElements();

        long totalIssues;
        long openIssues;
        long inProgressIssues;
        long inReviewIssues;
        long resolvedIssues;
        long closedIssues;

        if (isAdmin) {
            totalIssues = issueRepository.count();
            openIssues = issueRepository.countByStatus(IssueStatus.OPEN);
            inProgressIssues = issueRepository.countByStatus(IssueStatus.IN_PROGRESS);
            inReviewIssues = issueRepository.countByStatus(IssueStatus.IN_REVIEW);
            resolvedIssues = issueRepository.countByStatus(IssueStatus.RESOLVED);
            closedIssues = issueRepository.countByStatus(IssueStatus.CLOSED);
        } else {
            totalIssues = issueRepository.countAccessibleIssues(currentUser);
            openIssues = issueRepository.countAccessibleIssuesByStatus(currentUser, IssueStatus.OPEN);
            inProgressIssues = issueRepository.countAccessibleIssuesByStatus(currentUser, IssueStatus.IN_PROGRESS);
            inReviewIssues = issueRepository.countAccessibleIssuesByStatus(currentUser, IssueStatus.IN_REVIEW);
            resolvedIssues = issueRepository.countAccessibleIssuesByStatus(currentUser, IssueStatus.RESOLVED);
            closedIssues = issueRepository.countAccessibleIssuesByStatus(currentUser, IssueStatus.CLOSED);
        }

        long myAssignedIssues = issueRepository.countActiveByAssignee(currentUser);
        long overdueIssues = issueRepository.countOverdueByAssignee(currentUser, LocalDate.now());

        // Recent projects (top 5)
        List<Project> recentProjects = isAdmin
                ? projectRepository.findAll(PageRequest.of(0, 5)).getContent()
                : projectRepository.findProjectsByMember(currentUser, PageRequest.of(0, 5)).getContent();

        List<DashboardResponse.ProjectSummary> projectSummaries = recentProjects.stream()
                .map(p -> new DashboardResponse.ProjectSummary(
                        p.getId(), p.getName(), p.getProjectKey(),
                        projectRepository.countIssuesByProjectId(p.getId()),
                        issueRepository.countByProjectAndStatus(p, IssueStatus.OPEN)))
                .toList();

        // Recent issues
        List<IssueResponse> recentIssues = (isAdmin
                ? issueRepository.findTop10ByOrderByCreatedAtDesc()
                : issueRepository.findTop10ByAssigneeOrderByCreatedAtDesc(currentUser))
                .stream()
                .map(IssueResponse::from)
                .toList();

        return new DashboardResponse(
                totalProjects, totalIssues, openIssues, inProgressIssues, inReviewIssues,
                resolvedIssues, closedIssues, myAssignedIssues, overdueIssues,
                projectSummaries, recentIssues);
    }
}

