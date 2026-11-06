package com.jiralite.repository;

import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    Optional<Issue> findByIssueKey(String issueKey);

    Page<Issue> findByProject(Project project, Pageable pageable);

    Page<Issue> findByAssignee(User assignee, Pageable pageable);

    Page<Issue> findByReporter(User reporter, Pageable pageable);

    Page<Issue> findByProjectAndStatus(Project project, IssueStatus status, Pageable pageable);

    Page<Issue> findByProjectAndPriority(Project project, Priority priority, Pageable pageable);

    Page<Issue> findByProjectAndType(Project project, IssueType type, Pageable pageable);

    @Query("SELECT COALESCE(MAX(i.issueNumber), 0) FROM Issue i WHERE i.project = :project")
    Long findMaxIssueNumberByProject(@Param("project") Project project);

    @Query("SELECT i FROM Issue i WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.issueKey) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Issue> searchIssues(@Param("query") String query, Pageable pageable);

    long countByProject(Project project);

    long countByProjectAndStatus(Project project, IssueStatus status);

    long countByAssignee(User assignee);

    long countByStatus(IssueStatus status);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.assignee = :user AND i.status NOT IN ('RESOLVED','CLOSED')")
    long countActiveByAssignee(@Param("user") User user);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.dueDate < :today AND i.status NOT IN ('RESOLVED','CLOSED')")
    long countOverdueIssues(@Param("today") LocalDate today);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.assignee = :user AND i.dueDate < :today AND i.status NOT IN ('RESOLVED','CLOSED')")
    long countOverdueByAssignee(@Param("user") User user, @Param("today") LocalDate today);

    List<Issue> findTop10ByOrderByCreatedAtDesc();

    List<Issue> findTop10ByAssigneeOrderByCreatedAtDesc(User assignee);
}

