<div align="center">

<img src="docs/screenshots/02-dashboard.png" alt="Jira-Lite Dashboard" width="920"/>

# Jira-Lite

**A Production-Quality, Full-Stack Project Management and Issue Tracking Platform**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-06B6D4?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![CI Pipeline](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?logo=githubactions&logoColor=white)](.github/workflows/ci.yml)
[![Tests](https://img.shields.io/badge/Tests-27%2F27%20Passing-brightgreen?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[Features](#-key-features) • [Screenshots](#-application-screenshots) • [Architecture](#-system-architecture) • [Database Schema](#-database-er-diagram) • [Auth & RBAC](#-authentication--role-based-authorization) • [API Spec](#-api-documentation) • [Setup](#-installation--setup) • [Testing](#-testing)

</div>

---

## 📌 Overview

**Jira-Lite** is a modern, responsive issue tracking and agile project management system inspired by Atlassian Jira. Designed and engineered as a showcase software engineering portfolio application, it demonstrates production-grade full-stack patterns:

- **Type-safe enterprise architecture**: Java 21 and Spring Boot 4.1.1 backend paired with a React 19 + TypeScript + Vite 8 frontend.
- **Strict Data Isolation & Multi-Project RBAC**: Method-level security guards (`@PreAuthorize`) enforcing role permissions across `ADMIN`, `PROJECT_MANAGER`, and `DEVELOPER`.
- **Concurrent Update Safety**: JPA optimistic locking (`@Version`) preventing lost updates when multiple team members edit issues concurrently.
- **High-Throughput Caching**: Redis 7 cache abstraction (`@Cacheable` / `@CacheEvict`) for project boards and dashboard analytics with hardened type deserialization (`BasicPolymorphicTypeValidator`).
- **Interactive Kanban Workflow**: Drag-and-drop task transitions with immediate visual feedback, live search, and audit history.

---

## 📸 Application Screenshots

<table>
  <tr>
    <td width="50%" align="center">
      <img src="docs/screenshots/01-login.png" alt="Login & Quick Demo Fast-Fill" width="100%"/>
      <br/>
      <b>Authentication &amp; Role Quick-Login</b>
      <p><sub>JWT authentication with credential fast-fill buttons for Admin, PM, and Developer roles.</sub></p>
    </td>
    <td width="50%" align="center">
      <img src="docs/screenshots/02-dashboard.png" alt="Executive Dashboard" width="100%"/>
      <br/>
      <b>Executive Metrics Dashboard</b>
      <p><sub>Real-time project statistics, task completion rates, issue status distribution, and recent activity.</sub></p>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center">
      <img src="docs/screenshots/03-projects.png" alt="Projects Directory" width="100%"/>
      <br/>
      <b>Project Directory &amp; Management</b>
      <p><sub>Workspace projects overview with unique project keys, member counts, and ownership metadata.</sub></p>
    </td>
    <td width="50%" align="center">
      <img src="docs/screenshots/04-kanban-board.png" alt="Kanban Board" width="100%"/>
      <br/>
      <b>Interactive Kanban Board</b>
      <p><sub>Visual drag-and-drop across Open, In Progress, In Review, Resolved, and Closed stages.</sub></p>
    </td>
  </tr>
  <tr>
    <td width="50%" align="center">
      <img src="docs/screenshots/05-issue-detail.png" alt="Issue Details and Comments" width="100%"/>
      <br/>
      <b>Issue Lifecycle, Comments &amp; Audit Trail</b>
      <p><sub>Full issue view with optimistic lock versioning, threaded discussion, and historical audit log.</sub></p>
    </td>
    <td width="50%" align="center">
      <img src="docs/screenshots/06-team-members.png" alt="Team Members Directory" width="100%"/>
      <br/>
      <b>Workspace Team Directory</b>
      <p><sub>Member listing with role badges, email accounts, and project membership tracking.</sub></p>
    </td>
  </tr>
  <tr>
    <td colspan="2" align="center">
      <img src="docs/screenshots/07-notifications.png" alt="Notifications Popover" width="60%"/>
      <br/>
      <b>In-App Notification Center</b>
      <p><sub>Real-time unread counter, event-driven notification feed, and bulk read actions.</sub></p>
    </td>
  </tr>
</table>

---

## ⚡ Key Features

| Capability | Technical Implementation |
|---|---|
| **Stateless JWT Security** | JJWT (`0.12.6`) token provider with HMAC-SHA256 signature verification and BCrypt password encryption. |
| **Role-Based Access Control** | `@EnableMethodSecurity` with declarative `@PreAuthorize` rules on controllers and services. |
| **Interactive Kanban Board** | 5-stage lifecycle (`OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `RESOLVED`, `CLOSED`) with drag-and-drop state transitions. |
| **Optimistic Concurrency Control** | JPA `@Version` column on `Issue` preventing race conditions during simultaneous edits (HTTP 409 Conflict handling). |
| **Distributed Caching** | Redis 7 caching for projects, issues, and dashboard metrics with `@CacheEvict` cache invalidation on writes. |
| **Threaded Discussion** | Issue comments with author-level modification and deletion constraints. |
| **Immutable Audit Logging** | Dedicated `activity_logs` table tracking who created, changed status, commented, or assigned each task. |
| **Notification Center** | Event-driven in-app notifications for task assignments and status updates with unread count badges. |
| **Full-Text Filter & Search** | Debounced cross-project search by keyword, status, priority, and issue type with JPA query optimization. |
| **Pagination & Sorting** | Server-side Spring Data `Pageable` pagination across all collection endpoints. |
| **Interactive OpenAPI Docs** | Swagger UI accessible at `/swagger-ui.html` for exploratory testing. |
| **Multi-Stage Containerization** | Modular `Dockerfile` and `docker-compose.yml` for backend, frontend, PostgreSQL, and Redis. |

---

## 🏗️ System Architecture

The following diagram illustrates the complete end-to-end request lifecycle and component relationships:

```mermaid
flowchart TD
    subgraph Client["Frontend · React 19 + TypeScript + Vite 8"]
        direction TB
        UI["UI Pages & Components<br/>Tailwind CSS v4 · Lucide Icons"]
        CTX["AuthContext<br/>JWT State & LocalStorage"]
        AXIOS["Axios API Client<br/>Bearer Token Interceptor"]
        UI --> CTX
        CTX --> AXIOS
    end

    subgraph Ingress["Security & Ingress Tier"]
        direction TB
        CORS["CORS Filter (WebConfig)"]
        JWT_FILTER["JwtAuthenticationFilter<br/>Bearer Token Extraction & Validation"]
        AUTH_MGR["AuthenticationManager<br/>CustomUserDetailsService + BCrypt"]
        CORS --> JWT_FILTER
        JWT_FILTER --> AUTH_MGR
    end

    subgraph Backend["Application Tier · Spring Boot 4.1.1 (Java 21)"]
        direction TB
        CTRL["REST Controllers (7 Controllers)<br/>Auth · Project · Issue · Comment · Notification · Dashboard · User"]
        SVC["Service Layer (@Transactional · @PreAuthorize)<br/>Auth · Project · Issue (@Version) · Comment · Notification · Dashboard"]
        REPO["Data Access Layer<br/>7 Spring Data JPA Repositories + Custom JPQL"]
        CTRL --> SVC
        SVC --> REPO
    end

    subgraph Storage["Storage Tier"]
        REDIS[("Redis 7<br/>Spring Cache (BasicPolymorphicTypeValidator)")]
        PG[("PostgreSQL 16<br/>8 Relational Tables with Constraints & Indexes")]
    end

    AXIOS -->|HTTP REST / JSON| CORS
    AUTH_MGR --> CTRL
    SVC -->|Cache Read / Evict| REDIS
    REDIS -.->|Cached Data| SVC
    REPO -->|Hibernate ORM / JPQL| PG
    PG -.->|Query Results| REPO
```

### Request Lifecycle
1. **Client Dispatch**: The React application sends asynchronous HTTP requests via Axios with `Authorization: Bearer <JWT>`.
2. **Security Gateway**: `JwtAuthenticationFilter` intercepts the request, verifies signature and expiration via `JwtTokenProvider`, loads user authorities, and sets the Spring `SecurityContext`.
3. **Controller Guard**: Incoming requests are validated (`@Valid`) and checked against role permissions via `@PreAuthorize`.
4. **Cache Interception**: Services evaluate `@Cacheable` methods against Redis before querying the database.
5. **JPA Persistence**: Data mutations execute inside `@Transactional` boundaries. Updates check the `@Version` timestamp to guarantee optimistic locking integrity.
6. **Cache Eviction & Audit**: Write operations trigger `@CacheEvict` to flush stale caches, log events to `ActivityLog`, and generate `Notification` records.

---

## 🗄️ Database ER Diagram

The database schema is mapped by Hibernate and backed by PostgreSQL 16 (or in-memory H2 during testing):

```mermaid
erDiagram
    USERS ||--o{ PROJECTS : "owns"
    USERS ||--o{ PROJECT_MEMBERS : "joined"
    PROJECTS ||--o{ PROJECT_MEMBERS : "has"
    PROJECTS ||--o{ ISSUES : "contains"
    USERS ||--o{ ISSUES : "reported by"
    USERS ||--o{ ISSUES : "assigned to"
    ISSUES ||--o{ COMMENTS : "has"
    USERS ||--o{ COMMENTS : "authored by"
    PROJECTS ||--o{ ACTIVITY_LOGS : "project events"
    ISSUES ||--o{ ACTIVITY_LOGS : "issue events"
    USERS ||--o{ ACTIVITY_LOGS : "actor"
    USERS ||--o{ NOTIFICATIONS : "recipient"

    USERS {
        bigint id PK
        varchar first_name
        varchar last_name
        varchar email UK
        varchar password
        varchar role
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    PROJECTS {
        bigint id PK
        varchar name
        varchar description
        varchar project_key UK
        bigint owner_id FK
        timestamp created_at
        timestamp updated_at
    }

    PROJECT_MEMBERS {
        bigint id PK
        bigint project_id FK
        bigint user_id FK
        timestamp joined_at
    }

    ISSUES {
        bigint id PK
        varchar title
        text description
        varchar issue_key UK
        bigint issue_number
        varchar type
        varchar status
        varchar priority
        bigint project_id FK
        bigint reporter_id FK
        bigint assignee_id FK
        date due_date
        bigint version
        timestamp created_at
        timestamp updated_at
    }

    COMMENTS {
        bigint id PK
        text content
        bigint issue_id FK
        bigint author_id FK
        timestamp created_at
        timestamp updated_at
    }

    ACTIVITY_LOGS {
        bigint id PK
        varchar action
        varchar entity_type
        bigint entity_id
        varchar details
        bigint project_id FK
        bigint issue_id FK
        bigint user_id FK
        timestamp created_at
    }

    NOTIFICATIONS {
        bigint id PK
        varchar message
        varchar type
        bigint reference_id
        varchar reference_type
        bigint user_id FK
        boolean read
        timestamp created_at
    }
```

---

## 🔐 Authentication & Role-Based Authorization

### Role Hierarchy & Permissions

| Role | Scope & Permissions |
|---|---|
| `ADMIN` | Global platform administration. Can delete users, inspect all workspace projects, manage global configuration, and delete any issue or comment. |
| `PROJECT_MANAGER` | Can create new projects, add or remove project members, assign issues, edit project metadata, and manage sprint issues. |
| `DEVELOPER` | Can view accessible projects, create issues, transition issue statuses (`OPEN` ➔ `CLOSED`), comment on issues, and manage their assigned tasks. |

### How Demo Accounts & Quick-Login Buttons Work
- **Automatic Seed on First Boot**: When the application starts, `DataInitializer.java` checks whether `userRepository.count() == 0`. If the database is empty, it automatically seeds three workspace accounts (Admin, Project Manager, Developer) using passwords configured via environment variables.
- **Quick Demo Login Buttons**: On the `/login` screen, the **Admin**, **PM**, and **Dev** buttons are **client-side credential autofill helpers only**. They populate the email and password inputs with the standard test credentials. They do **not** bypass authentication: the user must submit the form, triggering a genuine `POST /api/auth/login` request, BCrypt password check, and JWT token issuance.
- **Disabling Account Seeding**: To run without seeding default accounts (e.g., in a production environment), set `app.seed.enabled=false`.

### User Registration Behaviour
- When registering via `POST /api/auth/register`, the backend service (`AuthService.java`) **strictly assigns `Role.DEVELOPER` by default**.
- Users **cannot** register themselves as `ADMIN` or `PROJECT_MANAGER`. Role escalation must be performed by an existing administrator.

---

## 📂 Project Structure

```
jira-lite/
├── frontend/                         # React 19 + TypeScript Frontend
│   ├── src/
│   │   ├── pages/                    # Application Views
│   │   │   ├── Login.tsx             # Auth form & demo autofill
│   │   │   ├── Register.tsx          # New user registration
│   │   │   ├── Dashboard.tsx         # Metrics, progress & activity stream
│   │   │   ├── Projects.tsx          # Projects catalog & create modal
│   │   │   ├── ProjectDetail.tsx     # Kanban board & list view
│   │   │   ├── IssueDetail.tsx       # Issue view, comments & audit log
│   │   │   ├── MyIssues.tsx          # Personal task list
│   │   │   ├── TeamMembers.tsx       # Workspace user directory
│   │   │   └── Settings.tsx          # User profile
│   │   ├── components/               # Reusable UI widgets (Navbar, Sidebar, Modal, Toast)
│   │   ├── context/AuthContext.tsx   # React Auth Context & session management
│   │   ├── services/api.ts           # Axios client with interceptors
│   │   └── types/index.ts            # TypeScript interfaces & enums
│   ├── Dockerfile                    # Multi-stage production build (Vite -> Nginx)
│   └── nginx.conf                    # Nginx reverse proxy & SPA routing
│
├── jira-lite/                        # Spring Boot 4.1.1 Backend
│   ├── src/main/java/com/jiralite/
│   │   ├── config/                   # SecurityConfig, RedisConfig, WebConfig, OpenApiConfig
│   │   ├── controller/               # REST API Controllers (7 controllers)
│   │   ├── dto/                      # Request & Response Records
│   │   ├── entity/                   # JPA Entities (8 entities)
│   │   ├── exception/                # GlobalExceptionHandler & custom exceptions
│   │   ├── model/                    # Enums (Role, IssueStatus, IssueType, Priority)
│   │   ├── repository/               # Spring Data JPA Repositories (7 interfaces)
│   │   ├── security/                 # JwtTokenProvider, JwtAuthenticationFilter, UserDetails
│   │   └── service/                  # Business logic & caching layer (8 services)
│   ├── src/main/resources/
│   │   └── application.yml           # Externalized configuration
│   ├── Dockerfile                    # Multi-stage build (Maven -> Eclipse Temurin 21 JRE)
│   └── pom.xml                       # Maven dependency management
│
├── docs/screenshots/                 # Genuine application screenshots
├── docker-compose.yml                # Multi-container orchestration
├── .env.example                      # Template for environment variables
└── README.md                         # Project documentation
```

---

## 📡 API Documentation

Interactive Swagger documentation is available at **`/swagger-ui.html`** when the backend is running.

### Key Endpoints

| Category | Method | Endpoint | Description | Role Required |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/auth/register` | Register new account (assigns `DEVELOPER`) | Public |
| **Auth** | `POST` | `/api/auth/login` | Authenticate and obtain JWT bearer token | Public |
| **Users** | `GET` | `/api/users/me` | Fetch authenticated user profile | Authenticated |
| **Users** | `GET` | `/api/users` | List all workspace users | Authenticated |
| **Users** | `DELETE`| `/api/users/{id}` | Deactivate user account | `ADMIN` |
| **Projects** | `GET` | `/api/projects` | List projects accessible to user (paginated) | Authenticated |
| **Projects** | `POST`| `/api/projects` | Create a new project | `ADMIN`, `PROJECT_MANAGER` |
| **Projects** | `GET` | `/api/projects/{id}` | Get project metadata and members | Authenticated |
| **Projects** | `POST`| `/api/projects/{id}/members` | Add user to project team | Project Owner / PM |
| **Projects** | `DELETE`| `/api/projects/{id}/members/{userId}` | Remove user from project | Project Owner / PM |
| **Projects** | `GET` | `/api/projects/{id}/activity` | Retrieve project audit logs | Authenticated |
| **Issues** | `POST`| `/api/issues/project/{id}` | Create issue in project | Project Member |
| **Issues** | `GET` | `/api/issues/project/{id}` | Query issues in project (filterable) | Authenticated |
| **Issues** | `GET` | `/api/issues/{id}` | Retrieve full issue details | Authenticated |
| **Issues** | `PUT` | `/api/issues/{id}` | Update issue title, desc, due date | Project Member |
| **Issues** | `PATCH`| `/api/issues/{id}/status` | Transition issue status (`OPEN` ➔ `CLOSED`) | Project Member |
| **Issues** | `PATCH`| `/api/issues/{id}/assign` | Assign issue to team member | Project Member |
| **Issues** | `GET` | `/api/issues/my` | Get current user's assigned issues | Authenticated |
| **Issues** | `GET` | `/api/issues/search?query=` | Full-text search across all issues | Authenticated |
| **Comments**| `POST`| `/api/issues/{id}/comments` | Post comment on issue | Project Member |
| **Comments**| `DELETE`| `/api/issues/{id}/comments/{cid}` | Remove comment | Author or `ADMIN` |
| **Notifs** | `GET` | `/api/notifications` | Get user notifications (paginated) | Authenticated |
| **Notifs** | `GET` | `/api/notifications/unread-count` | Get count of unread notifications | Authenticated |
| **Notifs** | `PATCH`| `/api/notifications/read-all` | Mark all notifications as read | Authenticated |
| **Dashboard**| `GET`| `/api/dashboard` | Aggregated system metrics | Authenticated |

---

## 🚀 Installation & Setup

### Prerequisites
- **Java 21 JDK** (e.g. Microsoft OpenJDK 21 or Eclipse Temurin 21)
- **Node.js 20+** and **npm**
- **PostgreSQL 16** and **Redis 7** (native services or via Docker)

---

### Method 1: Local Development (Native)

#### 1. Configure Infrastructure
Ensure PostgreSQL is listening on port `5432` and Redis on `6379`:
```sql
CREATE DATABASE jiralite;
CREATE USER jiralite WITH PASSWORD 'jiralite_secure_password';
GRANT ALL PRIVILEGES ON DATABASE jiralite TO jiralite;
```

#### 2. Start Spring Boot Backend
```bash
cd jira-lite

# Set environment variables (Linux/macOS)
export DB_USERNAME=jiralite
export DB_PASSWORD=jiralite_secure_password
export JWT_SECRET=bXlfc3VwZXJfc2VjcmV0X2tleV9mb3Jfand0X3Rva2VuX2dlbmVyYXRpb25fMjAyNg==
export SEED_ADMIN_PASSWORD=Admin@JiraLite2026!
export SEED_PM_PASSWORD=Pm@JiraLite2026!
export SEED_DEV_PASSWORD=Dev@JiraLite2026!

# Run backend (auto-creates tables and seeds default users)
./mvnw spring-boot:run
```
*(On Windows PowerShell, use `$env:DB_USERNAME="jiralite"`, etc., then `.\mvnw.cmd spring-boot:run`)*

The backend initializes on **`http://localhost:8080`**.

#### 3. Start React Frontend
```bash
cd frontend
npm install
npm run dev
```

The frontend dev server launches on **`http://localhost:5173`**.

---

### Method 2: Docker Compose Deployment

The repository includes a complete multi-container setup orchestrating the frontend, backend, PostgreSQL 16, and Redis 7.

```bash
# 1. Clone the repository and configure environment variables
cp .env.example .env
# Edit .env to set your passwords and secrets

# 2. Build and launch all services
docker compose --env-file .env up --build
```

- **Frontend**: `http://localhost:3000` (served by Nginx with API reverse proxy)
- **Backend API**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

> **Note on Environment Ports**: If PostgreSQL (`5432`) or Redis (`6379`) are already running natively on your host machine, modify `POSTGRES_PORT` and `REDIS_PORT` in your `.env` file (e.g., `5433` and `6380`) to prevent port conflicts.

---

## 🔧 Environment Variables

All sensitive values are strictly externalized. Never commit the `.env` file to version control.

| Variable | Description | Default / Example | Required |
|---|---|---|---|
| `DB_USERNAME` | PostgreSQL database user | `jiralite` | Yes |
| `DB_PASSWORD` | PostgreSQL database password | *Custom secure password* | Yes |
| `POSTGRES_DB` | PostgreSQL database name | `jiralite` | Yes |
| `JWT_SECRET` | Base64-encoded HMAC-SHA256 secret (>= 256 bits) | *Generate via openssl* | Yes |
| `SEED_ADMIN_PASSWORD` | Initial password for seeded Admin account | *Custom strong password* | Yes |
| `SEED_PM_PASSWORD` | Initial password for seeded PM account | *Custom strong password* | Yes |
| `SEED_DEV_PASSWORD` | Initial password for seeded Dev account | *Custom strong password* | Yes |
| `app.seed.enabled` | Whether to seed default accounts if DB is empty | `true` | No |
| `POSTGRES_PORT` | Host exposed port for PostgreSQL container | `5432` | Docker only |
| `REDIS_PORT` | Host exposed port for Redis container | `6379` | Docker only |
| `BACKEND_PORT` | Host exposed port for Spring Boot container | `8080` | Docker only |
| `FRONTEND_PORT` | Host exposed port for React/Nginx container | `3000` | Docker only |

---

## 🧪 Testing

The backend includes a comprehensive automated test suite verifying security, business services, controllers, and data isolation:

```bash
cd jira-lite
./mvnw test          # Linux/macOS
.\mvnw.cmd test      # Windows
```

### Test Suite Execution Summary

```text
[INFO] Running com.jiralite.JiraLiteApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiralite.controller.AuthControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiralite.service.AuthServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiralite.service.IssueServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiralite.service.ProjectServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.jiralite.service.SecurityAndAuthorizationTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------------------
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- **Security & RBAC Test Suite (`SecurityAndAuthorizationTest`)**: Validates cross-project isolation, assignee membership checks, non-owner PM mutation guards, scoped issue search, comment authorization, and self-deactivation prevention.
- **Zero External Dependencies**: Unit & service tests run against an isolated in-memory **H2 database** with `@ActiveProfiles("test")`.
- **Frontend Verification**: Fully passes `tsc -b && vite build` (2,139 modules transformed, 0 TypeScript errors).

---

## 🛠️ Technology Stack Breakdown

| Tier | Component | Selection & Rationale |
|---|---|---|
| **Language** | Java 21 | Modern LTS release leveraging records, pattern matching, and enhanced performance. |
| **Backend** | Spring Boot 4.1.1 | Enterprise application framework with Spring Security, Spring Data JPA, and Actuator. |
| **Security** | Spring Security 6 + JJWT | Stateless JWT token validation, BCrypt password hashing, and `@PreAuthorize` authorization. |
| **Data Tier** | PostgreSQL 16 | Relational ACID store with foreign key constraints, unique indexes, and optimistic locking. |
| **Cache Tier** | Redis 7 | Sub-millisecond response caching for boards and dashboard metrics with selective cache eviction. |
| **Frontend** | React 19 + TypeScript | Modern SPA architecture with strict type safety, modular components, and declarative routing. |
| **Bundler** | Vite 8 | Ultra-fast HMR and optimized production bundling. |
| **Styling** | Tailwind CSS v4 | Utility-first responsive design with consistent enterprise color system. |
| **Icons** | Lucide React | Lightweight, consistent iconography. |
| **Documentation**| SpringDoc OpenAPI 2.8.6 | Auto-generated OpenAPI 3.0 specification and interactive Swagger UI. |
| **DevOps** | Docker & Compose | Multi-stage production container builds served via Alpine Linux and Nginx. |

---

## 🔮 Future Enhancements

- [ ] **Sprint Burndown Analytics**: Charting sprint velocity, scope creep, and ideal burn-down progress.
- [ ] **Real-Time WebSockets**: Live board card updates using STOMP over WebSocket to eliminate polling.
- [ ] **Issue Dependencies & Subtasks**: Hierarchical issue relationships (`blocks`, `is blocked by`, parent-child epics).
- [ ] **Attachment Storage**: S3-compatible object storage integration for issue file attachments and screenshots.
- [ ] **External Webhook Integrations**: GitHub commit and pull request linkage to issues via commit message keys (e.g., `PRJ-101`).

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

<div align="center">
<sub>Engineered with precision as a full-stack portfolio demonstration.</sub>
</div>
