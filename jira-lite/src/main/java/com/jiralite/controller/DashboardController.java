package com.jiralite.controller;

import com.jiralite.dto.response.ApiResponse;
import com.jiralite.dto.response.DashboardResponse;
import com.jiralite.entity.User;
import com.jiralite.service.DashboardService;
import com.jiralite.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for aggregated project and issue statistics.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Project and issue metrics and summaries")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService,
                               UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get aggregated metrics and summaries for the current user's dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(currentUser)));
    }
}

