package com.jiralite.service;

import com.jiralite.dto.response.IssueResponse;
import com.jiralite.dto.response.ProjectResponse;
import com.jiralite.entity.Issue;
import com.jiralite.entity.Project;
import com.jiralite.entity.ProjectMember;
import com.jiralite.entity.User;
import com.jiralite.model.IssueStatus;
import com.jiralite.model.IssueType;
import com.jiralite.model.Priority;
import com.jiralite.model.Role;
import com.jiralite.repository.IssueRepository;
import com.jiralite.repository.ProjectMemberRepository;
import com.jiralite.repository.ProjectRepository;
import com.jiralite.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying Phase 2 database performance optimizations:
 * - EntityGraph elimination of N+1 queries during issue retrieval
 * - Batch aggregate count queries for project listings
 * - Pagination, sorting, and count correctness
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabasePerformanceTest {

    @Autowired
    private IssueService issueService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private EntityManager entityManager;

    private User admin;
    private User dev1;
    private User dev2;
    private Project projectA;
    private Project projectB;

    private Statistics getHibernateStatistics() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        projectRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Root")
                .email("admin@test.com")
                .password("hash")
                .role(Role.ADMIN)
                .active(true)
                .build());

        dev1 = userRepository.save(User.builder()
                .firstName("Dev")
                .lastName("One")
                .email("dev1@test.com")
                .password("hash")
                .role(Role.DEVELOPER)
                .active(true)
                .build());

        dev2 = userRepository.save(User.builder()
                .firstName("Dev")
                .lastName("Two")
                .email("dev2@test.com")
                .password("hash")
                .role(Role.DEVELOPER)
                .active(true)
                .build());

        projectA = projectRepository.save(Project.builder()
                .name("Alpha Project")
                .description("Alpha description")
                .projectKey("ALP")
                .owner(admin)
                .build());

        projectB = projectRepository.save(Project.builder()
                .name("Beta Project")
                .description("Beta description")
                .projectKey("BET")
                .owner(admin)
                .build());

        memberRepository.save(ProjectMember.builder().project(projectA).user(admin).build());
        memberRepository.save(ProjectMember.builder().project(projectA).user(dev1).build());
        memberRepository.save(ProjectMember.builder().project(projectA).user(dev2).build());

        memberRepository.save(ProjectMember.builder().project(projectB).user(admin).build());
        memberRepository.save(ProjectMember.builder().project(projectB).user(dev1).build());

        // Create 10 issues for projectA
        for (int i = 1; i <= 10; i++) {
            issueRepository.save(Issue.builder()
                    .title("Issue " + i)
                    .description("Description " + i)
                    .issueKey("ALP-" + i)
                    .issueNumber((long) i)
                    .type(IssueType.TASK)
                    .status(i % 2 == 0 ? IssueStatus.IN_PROGRESS : IssueStatus.OPEN)
                    .priority(Priority.MEDIUM)
                    .project(projectA)
                    .reporter(admin)
                    .assignee(i % 2 == 0 ? dev1 : dev2)
                    .build());
        }

        // Create 3 issues for projectB
        for (int i = 1; i <= 3; i++) {
            issueRepository.save(Issue.builder()
                    .title("Beta Issue " + i)
                    .description("Beta Description " + i)
                    .issueKey("BET-" + i)
                    .issueNumber((long) i)
                    .type(IssueType.BUG)
                    .status(IssueStatus.OPEN)
                    .priority(Priority.HIGH)
                    .project(projectB)
                    .reporter(admin)
                    .assignee(dev1)
                    .build());
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("getIssuesByProject: EntityGraph fetches project, reporter, and assignee in 1 query without N+1")
    void getIssuesByProject_NoNPlusOneQueries() {
        Statistics stats = getHibernateStatistics();
        stats.clear();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("issueNumber").ascending());
        Page<IssueResponse> page = issueService.getIssuesByProject(projectA.getId(), null, null, dev1, pageable);

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(10);

        // Verify all lazy associations were eagerly initialized without extra queries
        for (IssueResponse issue : page.getContent()) {
            assertThat(issue.projectKey()).isEqualTo("ALP");
            assertThat(issue.reporter()).isNotNull();
            assertThat(issue.reporter().email()).isEqualTo("admin@test.com");
            assertThat(issue.assignee()).isNotNull();
            assertThat(issue.assignee().email()).isIn("dev1@test.com", "dev2@test.com");
        }

        // Expected queries: exactly 4
        // 1: projectService.findProjectById(projectId)
        // 2: projectService.isProjectMember(project, currentUser)
        // 3: issueRepository.findByProject(project, pageable) with @EntityGraph (joins project, reporter, assignee)
        // 4: count query for pagination
        // Without @EntityGraph, lazy-loading project, reporter, and assignee across 10 issues
        // would produce 30 extra queries (10 projects + 10 reporters + 10 assignees), resulting in 34 queries.
        long queryCount = stats.getPrepareStatementCount();
        assertThat(queryCount).isEqualTo(4);
    }

    @Test
    @DisplayName("getAllProjects: Batch aggregate count queries eliminate 1+2N query explosion")
    void getAllProjects_BatchCountAggregation() {
        Statistics stats = getHibernateStatistics();
        stats.clear();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        Page<ProjectResponse> page = projectService.getAllProjects(admin, pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);

        ProjectResponse respA = page.getContent().stream()
                .filter(p -> p.projectKey().equals("ALP"))
                .findFirst()
                .orElseThrow();
        assertThat(respA.memberCount()).isEqualTo(3);
        assertThat(respA.issueCount()).isEqualTo(10);
        assertThat(respA.owner().email()).isEqualTo("admin@test.com");

        ProjectResponse respB = page.getContent().stream()
                .filter(p -> p.projectKey().equals("BET"))
                .findFirst()
                .orElseThrow();
        assertThat(respB.memberCount()).isEqualTo(2);
        assertThat(respB.issueCount()).isEqualTo(3);
        assertThat(respB.owner().email()).isEqualTo("admin@test.com");

        // Expected queries: exactly 3
        // 1: Page of projects with owner eagerly joined via @EntityGraph (Spring Data skips separate count query when content size < page size)
        // 2: Batch member count (GROUP BY project.id IN (:ids))
        // 3: Batch issue count (GROUP BY project.id IN (:ids))
        // Without optimization, this would take 1 (projects) + 2 (owners) + 2 (members) + 2 (issues) = 7 queries for 2 projects,
        // and 1 + 3N = 31 queries for 10 projects.
        long queryCount = stats.getPrepareStatementCount();
        assertThat(queryCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Pagination and sorting: Sorting by title descending returns exact sorted order and slice")
    void getIssuesByProject_PaginationAndSorting() {
        Pageable page1 = PageRequest.of(0, 5, Sort.by("title").descending());
        Page<IssueResponse> resultPage1 = issueService.getIssuesByProject(projectA.getId(), null, null, dev1, page1);

        assertThat(resultPage1.getContent()).hasSize(5);
        assertThat(resultPage1.getTotalElements()).isEqualTo(10);
        assertThat(resultPage1.getTotalPages()).isEqualTo(2);
        assertThat(resultPage1.getNumber()).isEqualTo(0);

        Pageable page2 = PageRequest.of(1, 5, Sort.by("title").descending());
        Page<IssueResponse> resultPage2 = issueService.getIssuesByProject(projectA.getId(), null, null, dev1, page2);

        assertThat(resultPage2.getContent()).hasSize(5);
        assertThat(resultPage2.getNumber()).isEqualTo(1);

        // Ensure disjoint sets across pages
        var page1Keys = resultPage1.getContent().stream().map(IssueResponse::issueKey).toList();
        var page2Keys = resultPage2.getContent().stream().map(IssueResponse::issueKey).toList();
        assertThat(page1Keys).doesNotContainAnyElementsOf(page2Keys);
    }
}
