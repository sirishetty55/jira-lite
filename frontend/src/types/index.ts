export type Role = 'ADMIN' | 'PROJECT_MANAGER' | 'DEVELOPER';

export type Priority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export type IssueStatus = 'OPEN' | 'IN_PROGRESS' | 'IN_REVIEW' | 'RESOLVED' | 'CLOSED';

export type IssueType = 'TASK' | 'BUG' | 'STORY' | 'EPIC';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  projectKey: string;
  owner: User;
  memberCount: number;
  issueCount: number;
  createdAt: string;
}

export interface ProjectMember {
  id: number;
  user: User;
  joinedAt: string;
}

export interface ProjectDetail extends Project {
  members: ProjectMember[];
  updatedAt?: string;
}

export interface Issue {
  id: number;
  issueKey: string;
  title: string;
  type: IssueType;
  status: IssueStatus;
  priority: Priority;
  projectId: number;
  projectKey: string;
  reporter: User;
  assignee?: User | null;
  dueDate?: string | null;
  version?: number;
  createdAt: string;
}

export interface IssueDetail extends Issue {
  description: string;
  projectName: string;
  commentCount: number;
  updatedAt?: string;
}

export interface Comment {
  id: number;
  content: string;
  author: User;
  createdAt: string;
  updatedAt?: string;
}

export interface ActivityLog {
  id: number;
  action: string;
  entityType: string;
  entityId: number;
  details: string;
  user: User;
  createdAt: string;
}

export interface Notification {
  id: number;
  message: string;
  type: string;
  referenceId?: number;
  referenceType?: string;
  read: boolean;
  createdAt: string;
}

export interface DashboardStats {
  totalProjects: number;
  totalIssues: number;
  openIssues: number;
  inProgressIssues: number;
  inReviewIssues: number;
  resolvedIssues: number;
  closedIssues: number;
  myAssignedIssues: number;
  overdueIssues: number;
  recentProjects: {
    id: number;
    name: string;
    projectKey: string;
    issueCount: number;
    openIssueCount: number;
  }[];
  recentIssues: Issue[];
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  empty: boolean;
}

