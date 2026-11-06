package com.jiralite.service;

import com.jiralite.dto.request.CreateCommentRequest;
import com.jiralite.dto.response.CommentResponse;
import com.jiralite.entity.Comment;
import com.jiralite.entity.Issue;
import com.jiralite.entity.User;
import com.jiralite.exception.AccessDeniedException;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.Role;
import com.jiralite.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comment CRUD operations on issues.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueService issueService;
    private final ProjectService projectService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          IssueService issueService,
                          ProjectService projectService,
                          ActivityLogService activityLogService,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.issueService = issueService;
        this.projectService = projectService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentResponse addComment(Long issueId, CreateCommentRequest request, User currentUser) {
        Issue issue = issueService.findIssueById(issueId);

        if (!projectService.isProjectMember(issue.getProject(), currentUser)) {
            throw new AccessDeniedException("You must be a project member to comment");
        }

        Comment comment = Comment.builder()
                .content(request.content())
                .issue(issue)
                .author(currentUser)
                .build();
        comment = commentRepository.save(comment);

        activityLogService.log("COMMENT_ADDED", "ISSUE", issue.getId(),
                "Comment added to " + issue.getIssueKey(),
                issue.getProject(), issue, currentUser);

        // Notify assignee (if different from commenter)
        if (issue.getAssignee() != null && !issue.getAssignee().getId().equals(currentUser.getId())) {
            notificationService.create(issue.getAssignee(), "COMMENT_ADDED",
                    currentUser.getFirstName() + " commented on " + issue.getIssueKey(),
                    issue.getId(), "ISSUE");
        }
        // Notify reporter (if different from commenter and assignee)
        if (!issue.getReporter().getId().equals(currentUser.getId())
                && (issue.getAssignee() == null || !issue.getReporter().getId().equals(issue.getAssignee().getId()))) {
            notificationService.create(issue.getReporter(), "COMMENT_ADDED",
                    currentUser.getFirstName() + " commented on " + issue.getIssueKey(),
                    issue.getId(), "ISSUE");
        }

        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByIssue(Long issueId, User currentUser, Pageable pageable) {
        Issue issue = issueService.findIssueById(issueId);
        if (!projectService.isProjectMember(issue.getProject(), currentUser)) {
            throw new AccessDeniedException("You do not have permission to view comments for this issue");
        }
        return commentRepository.findByIssueOrderByCreatedAtDesc(issue, pageable)
                .map(CommentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByIssue(Long issueId, Pageable pageable) {
        Issue issue = issueService.findIssueById(issueId);
        return commentRepository.findByIssueOrderByCreatedAtDesc(issue, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse updateComment(Long issueId, Long commentId, CreateCommentRequest request, User currentUser) {
        Comment comment = findCommentById(commentId);
        if (issueId != null && !comment.getIssue().getId().equals(issueId)) {
            throw new BadRequestException("Comment does not belong to the specified issue");
        }
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only edit your own comments");
        }
        comment.setContent(request.content());
        comment = commentRepository.save(comment);
        return CommentResponse.from(comment);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CreateCommentRequest request, User currentUser) {
        return updateComment(null, commentId, request, currentUser);
    }

    @Transactional
    public void deleteComment(Long issueId, Long commentId, User currentUser) {
        Comment comment = findCommentById(commentId);
        if (issueId != null && !comment.getIssue().getId().equals(issueId)) {
            throw new BadRequestException("Comment does not belong to the specified issue");
        }
        if (!comment.getAuthor().getId().equals(currentUser.getId())
                && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You can only delete your own comments");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        deleteComment(null, commentId, currentUser);
    }

    private Comment findCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", id));
    }
}

