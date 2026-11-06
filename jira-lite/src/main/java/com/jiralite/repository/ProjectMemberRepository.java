package com.jiralite.repository;

import com.jiralite.entity.Project;
import com.jiralite.entity.ProjectMember;
import com.jiralite.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Page<ProjectMember> findByProject(Project project, Pageable pageable);

    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    boolean existsByProjectAndUser(Project project, User user);

    void deleteByProjectAndUser(Project project, User user);

    long countByProject(Project project);
}

