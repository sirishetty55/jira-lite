package com.jiralite.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Comment entity for discussion threads on issues.
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_issue", columnList = "issue_id"),
        @Index(name = "idx_comments_author", columnList = "author_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Column(nullable = false, length = 5000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comments_issue"))
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comments_author"))
    private User author;
}

