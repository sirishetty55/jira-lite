package com.jiralite.controller;

import com.jiralite.dto.request.AddProjectMemberRequest;
import com.jiralite.dto.request.CreateProjectRequest;
import com.jiralite.dto.request.UpdateProjectRequest;
import com.jiralite.dto.response.ActivityLogResponse;
import com.jiralite.dto.response.ApiResponse;
import com.jiralite.dto.response.ProjectDetailResponse;
import com.jiralite.dto.response.ProjectMemberResponse;
import com.jiralite.dto.response.ProjectResponse;
import com.jiralite.entity.User;
import com.jiralite.service.ActivityLogService;
import com.jiralite.service.ProjectService;
import com.jiralite.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for project creation, management, membership, and activity logs.
 */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Project management and membership")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    public ProjectController(ProjectService projectService,
                             UserService userService,
                             ActivityLogService activityLogService) {
        this.projectService = projectService;
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Create a new project (Admin or Project Manager)")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        ProjectResponse response = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all projects accessible to the current user (paginated)")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getAllProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User currentUser = userService.getCurrentUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllProjects(currentUser, pageable)));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get project details including members and issue counts")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectById(id)));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Update an existing project's name or description")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        ProjectResponse response = projectService.updateProject(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", response));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete a project (Admin or owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        projectService.deleteProject(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully"));
    }

    // ── Members ────────────────────────────────────────────────

    @PostMapping("/{id:\\d+}/members")
    @Operation(summary = "Add a member to a project")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddProjectMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        ProjectMemberResponse response = projectService.addMember(id, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Member added successfully", response));
    }

    @GetMapping("/{id:\\d+}/members")
    @Operation(summary = "List all members of a project (paginated)")
    public ResponseEntity<ApiResponse<Page<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectMembers(id, pageable)));
    }

    @DeleteMapping("/{id:\\d+}/members/{userId:\\d+}")
    @Operation(summary = "Remove a member from a project")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        projectService.removeMember(id, userId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully"));
    }

    // ── Activity ───────────────────────────────────────────────

    @GetMapping("/{id:\\d+}/activity")
    @Operation(summary = "Get the audit activity log for a project (paginated)")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getProjectActivity(
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(activityLogService.getProjectActivity(id, pageable)));
    }
}

