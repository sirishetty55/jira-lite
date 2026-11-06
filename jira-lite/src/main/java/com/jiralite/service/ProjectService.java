package com.jiralite.service;

import com.jiralite.dto.request.AddProjectMemberRequest;
import com.jiralite.dto.request.CreateProjectRequest;
import com.jiralite.dto.request.UpdateProjectRequest;
import com.jiralite.dto.response.ProjectDetailResponse;
import com.jiralite.dto.response.ProjectMemberResponse;
import com.jiralite.dto.response.ProjectResponse;
import com.jiralite.entity.Project;
import com.jiralite.entity.ProjectMember;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.DuplicateResourceException;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.Role;
import com.jiralite.repository.ProjectMemberRepository;
import com.jiralite.repository.ProjectRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Project CRUD and membership management.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserService userService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          UserService userService,
                          ActivityLogService activityLogService,
                          NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    @CacheEvict(value = {"projects", "dashboard"}, allEntries = true)
    public ProjectResponse createProject(CreateProjectRequest request, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PROJECT_MANAGER) {
            throw new AccessDeniedException("Only Admins and Project Managers can create projects");
        }

        if (projectRepository.existsByProjectKey(request.projectKey())) {
            throw new DuplicateResourceException("Project", "key", request.projectKey());
        }

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .projectKey(request.projectKey())
                .owner(currentUser)
                .build();
        project = projectRepository.save(project);

        // Owner is automatically a member
        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(currentUser)
                .build();
        memberRepository.save(ownerMember);

        activityLogService.log("PROJECT_CREATED", "PROJECT", project.getId(),
                "Created project: " + project.getName(), project, null, currentUser);

        return ProjectResponse.from(project, 1, 0);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectById(Long id, User currentUser) {
        ProjectDetailResponse response = getCachedProjectDetail(id);
        if (currentUser.getRole() != Role.ADMIN) {
            Project project = findProjectById(id);
            if (!isProjectMember(project, currentUser)) {
                throw new AccessDeniedException("You do not have permission to view this project");
            }
        }
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "#id")
    public ProjectDetailResponse getCachedProjectDetail(Long id) {
        Project project = findProjectById(id);
        List<ProjectMember> members = memberRepository.findByProject(project, Pageable.unpaged()).getContent();
        long issueCount = projectRepository.countIssuesByProjectId(id);
        return ProjectDetailResponse.from(project, members, issueCount);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectById(Long id) {
        return getCachedProjectDetail(id);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(User currentUser, Pageable pageable) {
        Page<Project> projects;
        if (currentUser.getRole() == Role.ADMIN) {
            projects = projectRepository.findAll(pageable);
        } else {
            projects = projectRepository.findProjectsByMember(currentUser, pageable);
        }
        return projects.map(p -> ProjectResponse.from(p,
                projectRepository.countMembersByProjectId(p.getId()),
                projectRepository.countIssuesByProjectId(p.getId())));
    }

    @Transactional
    @CacheEvict(value = {"projects", "dashboard"}, allEntries = true)
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request, User currentUser) {
        Project project = findProjectById(id);
        if (currentUser.getRole() != Role.ADMIN && !project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only Admins or the project owner can update project metadata");
        }

        project.setName(request.name());
        project.setDescription(request.description());
        project = projectRepository.save(project);

        activityLogService.log("PROJECT_UPDATED", "PROJECT", project.getId(),
                "Updated project: " + project.getName(), project, null, currentUser);

        return ProjectResponse.from(project,
                projectRepository.countMembersByProjectId(id),
                projectRepository.countIssuesByProjectId(id));
    }

    @Transactional
    @CacheEvict(value = {"projects", "dashboard", "issues"}, allEntries = true)
    public void deleteProject(Long id, User currentUser) {
        Project project = findProjectById(id);
        if (currentUser.getRole() != Role.ADMIN && !project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only Admins or the project owner can delete projects");
        }
        projectRepository.delete(project);
    }

    @Transactional
    @CacheEvict(value = {"projects", "dashboard"}, allEntries = true)
    public ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request, User currentUser) {
        Project project = findProjectById(projectId);
        if (currentUser.getRole() != Role.ADMIN && !project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only Admins or the project owner can manage project members");
        }

        User newMember = userService.findUserById(request.userId());

        if (memberRepository.existsByProjectAndUser(project, newMember)) {
            throw new DuplicateResourceException("Member", "userId", request.userId());
        }

        ProjectMember pm = ProjectMember.builder()
                .project(project)
                .user(newMember)
                .build();
        pm = memberRepository.save(pm);

        notificationService.create(newMember, "MEMBER_ADDED",
                "You were added to project: " + project.getName(),
                project.getId(), "PROJECT");

        activityLogService.log("MEMBER_ADDED", "PROJECT", project.getId(),
                "Added " + newMember.getEmail() + " to project", project, null, currentUser);

        return ProjectMemberResponse.from(pm);
    }

    @Transactional
    @CacheEvict(value = {"projects", "dashboard"}, allEntries = true)
    public void removeMember(Long projectId, Long userId, User currentUser) {
        Project project = findProjectById(projectId);
        if (currentUser.getRole() != Role.ADMIN && !project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only Admins or the project owner can manage project members");
        }

        if (project.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Cannot remove the project owner");
        }

        User member = userService.findUserById(userId);
        memberRepository.deleteByProjectAndUser(project, member);

        activityLogService.log("MEMBER_REMOVED", "PROJECT", project.getId(),
                "Removed " + member.getEmail() + " from project", project, null, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<ProjectMemberResponse> getProjectMembers(Long projectId, User currentUser, Pageable pageable) {
        Project project = findProjectById(projectId);
        if (!isProjectMember(project, currentUser)) {
            throw new AccessDeniedException("You do not have permission to view members of this project");
        }
        return memberRepository.findByProject(project, pageable).map(ProjectMemberResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProjectMemberResponse> getProjectMembers(Long projectId, Pageable pageable) {
        Project project = findProjectById(projectId);
        return memberRepository.findByProject(project, pageable).map(ProjectMemberResponse::from);
    }

    // ── Helpers ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    public void verifyProjectAccess(Project project, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (project.getOwner().getId().equals(user.getId())) return;
        if (user.getRole() == Role.PROJECT_MANAGER &&
                memberRepository.existsByProjectAndUser(project, user)) return;

        throw new AccessDeniedException("You do not have permission to modify this project");
    }

    public boolean isProjectMember(Project project, User user) {
        if (user.getRole() == Role.ADMIN) return true;
        return project.getOwner().getId().equals(user.getId())
                || memberRepository.existsByProjectAndUser(project, user);
    }
}

