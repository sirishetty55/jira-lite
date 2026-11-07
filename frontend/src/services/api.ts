import axios from 'axios';
import type {
  ApiResponse,
  AuthResponse,
  Comment,
  DashboardStats,
  Issue,
  IssueDetail,
  IssueStatus,
  IssueType,
  PageResponse,
  Priority,
  Project,
  ProjectDetail,
  ProjectMember,
  User,
  ActivityLog,
  Notification
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL ?? (import.meta.env.PROD ? '/api' : 'http://localhost:8080/api');

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Auto-inject JWT Bearer token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 unauthenticated
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }
    return Promise.reject(error);
  }
);

// ── Auth APIs ──────────────────────────────────────────────────
export const authApi = {
  login: async (email: string, password: string):Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    return res.data.data;
  },
  register: async (firstName: string, lastName: string, email: string, password: string): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', {
      firstName,
      lastName,
      email,
      password,
    });
    return res.data.data;
  },
  getCurrentUser: async (): Promise<User> => {
    const res = await apiClient.get<ApiResponse<User>>('/users/me');
    return res.data.data;
  },
  getAllUsers: async (page = 0, size = 50): Promise<PageResponse<User>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<User>>>(`/users?page=${page}&size=${size}`);
    return res.data.data;
  },
};

// ── Projects APIs ──────────────────────────────────────────────
export const projectsApi = {
  getAll: async (page = 0, size = 20): Promise<PageResponse<Project>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Project>>>(`/projects?page=${page}&size=${size}`);
    return res.data.data;
  },
  getById: async (id: number): Promise<ProjectDetail> => {
    const res = await apiClient.get<ApiResponse<ProjectDetail>>(`/projects/${id}`);
    return res.data.data;
  },
  create: async (name: string, description: string, projectKey: string): Promise<Project> => {
    const res = await apiClient.post<ApiResponse<Project>>('/projects', { name, description, projectKey });
    return res.data.data;
  },
  update: async (id: number, name: string, description: string): Promise<Project> => {
    const res = await apiClient.put<ApiResponse<Project>>(`/projects/${id}`, { name, description });
    return res.data.data;
  },
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/projects/${id}`);
  },
  addMember: async (projectId: number, userId: number): Promise<ProjectMember> => {
    const res = await apiClient.post<ApiResponse<ProjectMember>>(`/projects/${projectId}/members`, { userId });
    return res.data.data;
  },
  removeMember: async (projectId: number, userId: number): Promise<void> => {
    await apiClient.delete(`/projects/${projectId}/members/${userId}`);
  },
  getMembers: async (projectId: number): Promise<PageResponse<ProjectMember>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<ProjectMember>>>(`/projects/${projectId}/members`);
    return res.data.data;
  },
  getActivity: async (projectId: number, page = 0, size = 20): Promise<PageResponse<ActivityLog>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<ActivityLog>>>(`/projects/${projectId}/activity?page=${page}&size=${size}`);
    return res.data.data;
  },
};

// ── Issues APIs ────────────────────────────────────────────────
export const issuesApi = {
  getByProject: async (
    projectId: number,
    status?: IssueStatus,
    search?: string,
    page = 0,
    size = 50
  ): Promise<PageResponse<Issue>> => {
    let url = `/issues/project/${projectId}?page=${page}&size=${size}`;
    if (status) url += `&status=${status}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    const res = await apiClient.get<ApiResponse<PageResponse<Issue>>>(url);
    return res.data.data;
  },
  getById: async (id: number): Promise<IssueDetail> => {
    const res = await apiClient.get<ApiResponse<IssueDetail>>(`/issues/${id}`);
    return res.data.data;
  },
  create: async (
    projectId: number,
    data: {
      title: string;
      description?: string;
      type: IssueType;
      priority: Priority;
      assigneeId?: number | null;
      dueDate?: string | null;
    }
  ): Promise<Issue> => {
    const res = await apiClient.post<ApiResponse<Issue>>(`/issues/project/${projectId}`, data);
    return res.data.data;
  },
  update: async (
    id: number,
    data: {
      title: string;
      description?: string;
      type: IssueType;
      priority: Priority;
      assigneeId?: number | null;
      dueDate?: string | null;
      version?: number | null;
    }
  ): Promise<Issue> => {
    const res = await apiClient.put<ApiResponse<Issue>>(`/issues/${id}`, data);
    return res.data.data;
  },
  updateStatus: async (id: number, status: IssueStatus): Promise<Issue> => {
    const res = await apiClient.patch<ApiResponse<Issue>>(`/issues/${id}/status`, { status });
    return res.data.data;
  },
  assign: async (id: number, assigneeId?: number | null): Promise<Issue> => {
    const res = await apiClient.patch<ApiResponse<Issue>>(`/issues/${id}/assign`, { assigneeId });
    return res.data.data;
  },
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/issues/${id}`);
  },
  getMyIssues: async (page = 0, size = 50): Promise<PageResponse<Issue>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Issue>>>(`/issues/my?page=${page}&size=${size}`);
    return res.data.data;
  },
  search: async (query: string, page = 0, size = 20): Promise<PageResponse<Issue>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Issue>>>(`/issues/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    return res.data.data;
  },
  getActivity: async (issueId: number): Promise<PageResponse<ActivityLog>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<ActivityLog>>>(`/issues/${issueId}/activity`);
    return res.data.data;
  },
};

// ── Comments APIs ──────────────────────────────────────────────
export const commentsApi = {
  getByIssue: async (issueId: number, page = 0, size = 50): Promise<PageResponse<Comment>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Comment>>>(`/issues/${issueId}/comments?page=${page}&size=${size}`);
    return res.data.data;
  },
  add: async (issueId: number, content: string): Promise<Comment> => {
    const res = await apiClient.post<ApiResponse<Comment>>(`/issues/${issueId}/comments`, { content });
    return res.data.data;
  },
  update: async (issueId: number, commentId: number, content: string): Promise<Comment> => {
    const res = await apiClient.put<ApiResponse<Comment>>(`/issues/${issueId}/comments/${commentId}`, { content });
    return res.data.data;
  },
  delete: async (issueId: number, commentId: number): Promise<void> => {
    await apiClient.delete(`/issues/${issueId}/comments/${commentId}`);
  },
};

// ── Notifications APIs ─────────────────────────────────────────
export const notificationsApi = {
  getAll: async (page = 0, size = 20): Promise<PageResponse<Notification>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Notification>>>(`/notifications?page=${page}&size=${size}`);
    return res.data.data;
  },
  getUnreadCount: async (): Promise<number> => {
    const res = await apiClient.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
    return res.data.data.unreadCount;
  },
  markAsRead: async (id: number): Promise<void> => {
    await apiClient.patch(`/notifications/${id}/read`);
  },
  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch('/notifications/read-all');
  },
};

// ── Dashboard APIs ─────────────────────────────────────────────
export const dashboardApi = {
  getStats: async (): Promise<DashboardStats> => {
    const res = await apiClient.get<ApiResponse<DashboardStats>>('/dashboard');
    return res.data.data;
  },
};

