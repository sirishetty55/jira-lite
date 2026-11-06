package com.jiralite.controller;

import com.jiralite.dto.request.AssignIssueRequest;
import com.jiralite.dto.request.CreateIssueRequest;
import com.jiralite.dto.request.UpdateIssueRequest;
import com.jiralite.dto.request.UpdateIssueStatusRequest;
import com.jiralite.dto.response.ActivityLogResponse;
import com.jiralite.dto.response.ApiResponse;
import com.jiralite.dto.response.IssueDetailResponse;
import com.jiralite.dto.response.IssueResponse;
import com.jiralite.entity.User;
import com.jiralite.model.IssueStatus;
import com.jiralite.service.ActivityLogService;
import com.jiralite.service.IssueService;
import com.jiralite.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for issue lifecycle management, assignment, status, and search.
 */
@RestController
@RequestMapping("/api/issues")
@Tag(name = "Issues", description = "Issue management, assignment, status tracking, search")
public class IssueController {

    private final IssueService issueService;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    public IssueController(IssueService issueService,
                           UserService userService,
                           ActivityLogService activityLogService) {
        this.issueService = issueService;
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/project/{projectId}")
    @Operation(summary = "Create a new issue within a project")
    public ResponseEntity<ApiResponse<IssueResponse>> createIssue(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        IssueResponse response = issueService.createIssue(projectId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Issue created successfully", response));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List issues in a project with optional status filter and search query (paginated)")
    public ResponseEntity<ApiResponse<Page<IssueResponse>>> getIssuesByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                issueService.getIssuesByProject(projectId, status, search, pageable)));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get full issue details by ID")
    public ResponseEntity<ApiResponse<IssueDetailResponse>> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(issueService.getIssueById(id)));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Update an issue's details")
    public ResponseEntity<ApiResponse<IssueResponse>> updateIssue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        IssueResponse response = issueService.updateIssue(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Issue updated successfully", response));
    }

    @PatchMapping("/{id:\\d+}/status")
    @Operation(summary = "Transition an issue's lifecycle status")
    public ResponseEntity<ApiResponse<IssueResponse>> updateIssueStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        IssueResponse response = issueService.updateIssueStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", response));
    }

    @PatchMapping("/{id:\\d+}/assign")
    @Operation(summary = "Assign or unassign an issue")
    public ResponseEntity<ApiResponse<IssueResponse>> assignIssue(
            @PathVariable Long id,
            @RequestBody AssignIssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        IssueResponse response = issueService.assignIssue(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Issue assignment updated", response));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete an issue")
    public ResponseEntity<ApiResponse<Void>> deleteIssue(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        issueService.deleteIssue(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Issue deleted successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search across issue title, description, and key")
    public ResponseEntity<ApiResponse<Page<IssueResponse>>> searchIssues(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(issueService.searchIssues(query, pageable)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get issues assigned to the current user (paginated)")
    public ResponseEntity<ApiResponse<Page<IssueResponse>>> getMyIssues(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User currentUser = userService.getCurrentUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(issueService.getMyIssues(currentUser, pageable)));
    }

    @GetMapping("/{id:\\d+}/activity")
    @Operation(summary = "Get the audit activity log for an issue (paginated)")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getIssueActivity(
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(activityLogService.getIssueActivity(id, pageable)));
    }
}

