package com.jiralite.repository;

import com.jiralite.entity.Project;
import com.jiralite.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Override
    @EntityGraph(attributePaths = {"owner"})
    Page<Project> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"owner"})
    Optional<Project> findById(Long id);

    @EntityGraph(attributePaths = {"owner"})
    Optional<Project> findByProjectKey(String projectKey);

    boolean existsByProjectKey(String projectKey);

    @EntityGraph(attributePaths = {"owner"})
    Page<Project> findByOwner(User owner, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    @Query("SELECT p FROM Project p WHERE p.owner = :user OR EXISTS " +
           "(SELECT pm FROM ProjectMember pm WHERE pm.project = p AND pm.user = :user)")
    Page<Project> findProjectsByMember(@Param("user") User user, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    @Query("SELECT p FROM Project p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.projectKey) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Project> searchProjects(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.project.id = :projectId")
    long countIssuesByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId")
    long countMembersByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT pm.project.id, COUNT(pm) FROM ProjectMember pm WHERE pm.project.id IN :projectIds GROUP BY pm.project.id")
    List<Object[]> countMembersByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    @Query("SELECT i.project.id, COUNT(i) FROM Issue i WHERE i.project.id IN :projectIds GROUP BY i.project.id")
    List<Object[]> countIssuesByProjectIds(@Param("projectIds") Collection<Long> projectIds);
}
