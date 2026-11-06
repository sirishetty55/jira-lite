# Jira-Lite — Team Project and Issue Tracking Platform

Jira-Lite is a portfolio-level, full-stack project management and issue tracking system engineered with **Spring Boot 4.1.1 (Java 21)**, **PostgreSQL 16**, **Redis 7**, and **React 19 (TypeScript + Vite)**.

---

## 🚀 Features

* **User Authentication & RBAC**: JWT stateless bearer token authentication, secure BCrypt password hashing, and role-based permissions (`ADMIN`, `PROJECT_MANAGER`, `DEVELOPER`).
* **Interactive Kanban Board**: Visual drag-and-drop workflow across `OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `RESOLVED`, and `CLOSED` states with optimistic UI updates.
* **Project & Membership Management**: Unique project keys (`PHX`), member access control, and ownership permissions.
* **Issue Lifecycle & Concurrency**: Create, edit, assign, prioritize (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), and set deadlines with **JPA optimistic locking** (`@Version`) preventing race conditions.
* **Discussion Threads**: Markdown-ready comment threads on issues with author-level edit and delete controls.
* **Live Audit History**: Complete audit trail tracking who created, updated, transitioned, commented, or assigned issues.
* **Notification Center**: Real-time unread badges and notification popover for issue assignments, mentions, and resolutions.
* **Executive Metrics Dashboard**: Aggregated real-time metrics, project delivery progress bars, overdue alerts, and personal sprint queue.
* **Full-Text Search & Multi-Filters**: Debounced search across task titles, keys, and assignees with status and priority filtering.
* **Distributed Caching**: High-throughput Redis caching for projects, issues, and dashboard metrics with automatic invalidation.
* **Interactive API Documentation**: Live Swagger/OpenAPI documentation at `/swagger-ui.html`.

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|---|---|---|
| **Backend Framework** | Spring Boot | `4.1.1` |
| **Language** | Java | `21` |
| **Security** | Spring Security + JJWT | `0.12.6` |
| **ORM & Data** | Spring Data JPA / Hibernate | `6.x` |
| **Relational Database** | PostgreSQL | `16` |
| **In-Memory Test DB** | H2 Database | Latest |
| **Distributed Cache** | Redis | `7` |
| **API Documentation** | SpringDoc OpenAPI (Swagger UI) | `2.8.6` |
| **Frontend Framework** | React + TypeScript + Vite | `19` / `Vite 8` |
| **Styling & Icons** | Tailwind CSS v4 + Lucide React | Latest |
| **Containerization** | Docker & Docker Compose | Multi-stage |
| **Testing** | JUnit 5 + Mockito + AssertJ | Latest |

---

## 📋 API Specification

### Authentication & Users
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user account | Public |
| `POST` | `/api/auth/login` | Authenticate & retrieve JWT token | Public |
| `GET` | `/api/users/me` | Get current user profile | Authenticated |
| `GET` | `/api/users` | List all workspace users | Authenticated |
| `DELETE` | `/api/users/{id}` | Deactivate user account | `ADMIN` only |

### Projects & Membership
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/projects` | List projects accessible to user | Authenticated |
| `POST` | `/api/projects` | Create a new project | `ADMIN`, `PROJECT_MANAGER` |
| `GET` | `/api/projects/{id}` | Get project detail & member list | Authenticated |
| `PUT` | `/api/projects/{id}` | Update project metadata | Project Owner / `ADMIN` |
| `DELETE` | `/api/projects/{id}` | Delete project | Project Owner / `ADMIN` |
| `POST` | `/api/projects/{id}/members` | Add user to project | Project Owner / PM |
| `DELETE` | `/api/projects/{id}/members/{userId}` | Remove user from project | Project Owner / PM |
| `GET` | `/api/projects/{id}/activity` | Get project audit activity | Authenticated |

### Issues & Tasks
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/issues/project/{projectId}` | Create an issue in project | Project Member |
| `GET` | `/api/issues/project/{projectId}` | Filter/search issues in project | Authenticated |
| `GET` | `/api/issues/{id}` | Get full issue details | Authenticated |
| `PUT` | `/api/issues/{id}` | Update issue title & description | Project Member |
| `PATCH`| `/api/issues/{id}/status` | Transition status (`OPEN` ➔ `CLOSED`) | Project Member |
| `PATCH`| `/api/issues/{id}/assign` | Assign/unassign issue | Project Member |
| `DELETE`| `/api/issues/{id}` | Delete issue | Project Member |
| `GET` | `/api/issues/my` | Get current user's assigned issues | Authenticated |
| `GET` | `/api/issues/search?query=...` | Full-text search across all issues | Authenticated |
| `GET` | `/api/issues/{id}/activity` | Get issue-specific audit history | Authenticated |

### Comments & Discussions
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/issues/{issueId}/comments` | Post a comment on an issue | Project Member |
| `GET` | `/api/issues/{issueId}/comments` | List comments (paginated) | Authenticated |
| `PUT` | `/api/issues/{issueId}/comments/{commentId}` | Edit comment | Author only |
| `DELETE`| `/api/issues/{issueId}/comments/{commentId}` | Delete comment | Author or `ADMIN` |

### Notifications & Dashboard
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/notifications` | Get user notifications | Authenticated |
| `GET` | `/api/notifications/unread-count` | Get unread count badge | Authenticated |
| `PATCH`| `/api/notifications/{id}/read` | Mark notification as read | Recipient |
| `PATCH`| `/api/notifications/read-all` | Mark all notifications read | Recipient |
| `GET` | `/api/dashboard` | Aggregated metrics & summaries | Authenticated |

---

## 🏃 Setup & Running Instructions

### Prerequisites
* **Java 21 JDK** (e.g. Microsoft OpenJDK 21 or Eclipse Temurin 21)
* **Node.js 20+** and **npm**
* **Docker & Docker Compose** (for containerized setup)

---

### Option 1: One-Click Run with Docker Compose

**Step 1:** Copy the environment template and set your own passwords and secrets:
```bash
cp .env.example .env
# Edit .env — replace all placeholder values with real credentials before starting
```

**Step 2:** Launch the complete platform (PostgreSQL, Redis, Spring Boot backend, and React frontend with Nginx):
```bash
docker compose --env-file .env up --build
```

* Frontend UI: [http://localhost:3000](http://localhost:3000)
* Backend API: [http://localhost:8080](http://localhost:8080)
* Swagger UI Docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

> **Note:** If you have PostgreSQL or Redis already running on ports 5432/6379, adjust `POSTGRES_PORT` and `REDIS_PORT` in your `.env` to avoid conflicts (e.g. `POSTGRES_PORT=5433`).

---

### Option 2: Local Development Setup

#### 1. Start Infrastructure (Postgres & Redis)
```bash
docker-compose up -d postgres redis
```

#### 2. Start Spring Boot Backend
```bash
cd jira-lite
./mvnw spring-boot:run
```
*(On Windows: `.\mvnw.cmd spring-boot:run`)*

The backend will automatically start on `http://localhost:8080` and seed the initial default administrator.

#### 3. Start React Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend dev server will launch at `http://localhost:5173`.

---

## 🔐 Default Credentials

The platform automatically seeds an administrator account upon initial startup:

| Role | Email | Default Password (from `.env`) |
|---|---|---|
| **Admin** | `admin@jiralite.com` | Set via `SEED_ADMIN_PASSWORD` |
| **Project Manager** | `pm@jiralite.com` | Set via `SEED_PM_PASSWORD` |
| **Developer** | `dev@jiralite.com` | Set via `SEED_DEV_PASSWORD` |

> **⚠️ Security:** Seed passwords are loaded from environment variables at startup. Never use the defaults in production — set strong unique values in your `.env` file.

You can also use the **Quick Demo Accounts** buttons directly on the Login screen to test with different roles!

---

## 🧪 Testing

To run the comprehensive JUnit 5 and Mockito test suite:

```bash
cd jira-lite
./mvnw test
```

All 14 unit and integration test suites run against an in-memory H2 database with zero external dependencies.

---

## 🔧 Environment Variables

Copy `.env.example` to `.env` and customize before running:

| Variable | Description | Default (example) |
|---|---|---|
| `POSTGRES_PORT` | Host port for PostgreSQL | `5432` |
| `REDIS_PORT` | Host port for Redis | `6379` |
| `BACKEND_PORT` | Host port for the backend API | `8080` |
| `FRONTEND_PORT` | Host port for the frontend | `3000` |
| `DB_USERNAME` | PostgreSQL username | `jiralite` |
| `DB_PASSWORD` | PostgreSQL password | *(set a strong value)* |
| `POSTGRES_DB` | PostgreSQL database name | `jiralite` |
| `JWT_SECRET` | Base64-encoded 256-bit JWT secret | *(generate your own)* |
| `SEED_ADMIN_PASSWORD` | Password for seeded admin account | *(set a strong value)* |
| `SEED_PM_PASSWORD` | Password for seeded PM account | *(set a strong value)* |
| `SEED_DEV_PASSWORD` | Password for seeded developer account | *(set a strong value)* |

> **Never commit your `.env` file.** It is listed in `.gitignore`. Only `.env.example` (with safe placeholder values) is tracked in Git.

---

## 📄 License

This project is a portfolio demonstration and is provided for educational purposes.

