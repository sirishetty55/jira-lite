package com.jiralite.repository;

import com.jiralite.entity.Comment;
import com.jiralite.entity.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByIssue(Issue issue, Pageable pageable);

    Page<Comment> findByIssueOrderByCreatedAtDesc(Issue issue, Pageable pageable);

    long countByIssue(Issue issue);
}

