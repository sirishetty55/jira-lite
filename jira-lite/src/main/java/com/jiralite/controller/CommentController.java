package com.jiralite.controller;

import com.jiralite.dto.request.CreateCommentRequest;
import com.jiralite.dto.response.ApiResponse;
import com.jiralite.dto.response.CommentResponse;
import com.jiralite.entity.User;
import com.jiralite.service.CommentService;
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
 * Endpoints for adding, viewing, editing, and deleting comments on issues.
 */
@RestController
@RequestMapping("/api/issues/{issueId}/comments")
@Tag(name = "Comments", description = "Issue comment threads")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Post a comment on an issue")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long issueId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        CommentResponse response = commentService.addComment(issueId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", response));
    }

    @GetMapping
    @Operation(summary = "List comments for an issue (paginated, latest first)")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable Long issueId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByIssue(issueId, pageable)));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Edit an existing comment (author only)")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        CommentResponse response = commentService.updateComment(commentId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment (author or Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long issueId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getCurrentUser(userDetails);
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully"));
    }
}

