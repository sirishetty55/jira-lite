package com.jiralite.entity;

import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Issue entity representing a task, bug, story, or epic within a project.
 */
@Entity
@Table(name = "issues", indexes = {
        @Index(name = "idx_issues_key", columnList = "issue_key", unique = true),
        @Index(name = "idx_issues_project", columnList = "project_id"),
        @Index(name = "idx_issues_assignee", columnList = "assignee_id"),
        @Index(name = "idx_issues_reporter", columnList = "reporter_id"),
        @Index(name = "idx_issues_status", columnList = "status"),
        @Index(name = "idx_issues_priority", columnList = "priority"),
        @Index(name = "idx_issues_project_status", columnList = "project_id, status"),
        @Index(name = "idx_issues_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(name = "issue_key", nullable = false, unique = true, length = 20)
    private String issueKey;

    @Column(name = "issue_number", nullable = false)
    private Long issueNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private IssueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_issues_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_issues_reporter"))
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id",
            foreignKey = @ForeignKey(name = "fk_issues_assignee"))
    private User assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}

