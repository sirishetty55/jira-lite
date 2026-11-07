import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProjectDetail } from './ProjectDetail';
import { projectsApi, issuesApi, authApi } from '../services/api';
import * as ToastModule from '../components/Toast';
import type { ProjectDetail as ProjectDetailType, Issue } from '../types';

vi.mock('../services/api', () => ({
  projectsApi: {
    getById: vi.fn(),
    getActivity: vi.fn(),
  },
  issuesApi: {
    getByProject: vi.fn(),
    updateStatus: vi.fn(),
    create: vi.fn(),
  },
  authApi: {
    getAllUsers: vi.fn(),
  },
}));

const mockProject: ProjectDetailType = {
  id: 1,
  name: 'Test Project',
  projectKey: 'TEST',
  description: 'Test project description',
  memberCount: 0,
  issueCount: 1,
  owner: {
    id: 1,
    firstName: 'Admin',
    lastName: 'User',
    email: 'admin@test.com',
    role: 'ADMIN',
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
  },
  members: [],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const mockIssues: Issue[] = [
  {
    id: 101,
    title: 'First Test Task',
    issueKey: 'TEST-1',
    type: 'TASK',
    status: 'OPEN',
    priority: 'HIGH',
    projectId: 1,
    projectKey: 'TEST',
    reporter: mockProject.owner,
    assignee: null,
    createdAt: '2026-01-01T00:00:00Z',
  },
];

describe('ProjectDetail Component', () => {
  const mockToastSuccess = vi.fn();
  const mockToastError = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    vi.spyOn(ToastModule, 'useToast').mockReturnValue({
      toast: vi.fn(),
      success: mockToastSuccess,
      error: mockToastError,
      info: vi.fn(),
    });

    vi.mocked(projectsApi.getById).mockResolvedValue(mockProject);
    vi.mocked(issuesApi.getByProject).mockResolvedValue({
      content: [...mockIssues],
      totalPages: 1,
      totalElements: 1,
      size: 100,
      number: 0,
      empty: false,
    });
    vi.mocked(projectsApi.getActivity).mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      size: 30,
      number: 0,
      empty: true,
    });
    vi.mocked(authApi.getAllUsers).mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      size: 100,
      number: 0,
      empty: true,
    });
  });

  const renderComponent = (projectId = '1') => {
    return render(
      <MemoryRouter initialEntries={[`/projects/${projectId}`]}>
        <Routes>
          <Route path="/projects/:id" element={<ProjectDetail />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('displays loading spinner before project data resolves', () => {
    vi.mocked(projectsApi.getById).mockReturnValue(new Promise(() => {})); // Never resolves
    vi.mocked(authApi.getAllUsers).mockReturnValue(new Promise(() => {})); // Never resolves

    const { container } = renderComponent();

    expect(container.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('renders project detail, tabs, and Kanban board columns once loaded', async () => {
    renderComponent();

    expect(await screen.findByRole('heading', { name: 'Test Project' })).toBeInTheDocument();
    expect(screen.getAllByText('TEST').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Kanban Board')).toBeInTheDocument();
    expect(screen.getByText('First Test Task')).toBeInTheDocument();

    // Verify Kanban columns
    expect(screen.getByText('Open')).toBeInTheDocument();
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    expect(screen.getByText('In Review')).toBeInTheDocument();
    expect(screen.getByText('Resolved')).toBeInTheDocument();
    expect(screen.getByText('Closed')).toBeInTheDocument();
  });

  it('optimistically moves an issue to new status and confirms on success', async () => {
    vi.mocked(issuesApi.updateStatus).mockResolvedValueOnce({
      ...mockIssues[0],
      status: 'IN_PROGRESS',
    });

    renderComponent();

    const taskCard = await screen.findByText('First Test Task');
    expect(taskCard).toBeInTheDocument();

    // Drag start
    fireEvent.dragStart(taskCard);

    // Drop onto IN_PROGRESS column
    const inProgressHeader = screen.getByText('In Progress');
    const inProgressColumn = inProgressHeader.closest('div[class*="min-h-"]');
    expect(inProgressColumn).not.toBeNull();

    fireEvent.drop(inProgressColumn!);

    // API called with new status
    expect(issuesApi.updateStatus).toHaveBeenCalledWith(101, 'IN_PROGRESS');

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith(expect.stringContaining('Moved TEST-1 to IN PROGRESS'));
    });
  });

  it('rolls back optimistic update when API update fails', async () => {
    // API fails with 500 error
    vi.mocked(issuesApi.updateStatus).mockRejectedValueOnce({
      response: {
        data: {
          message: 'Concurrent update conflict',
        },
      },
    });

    renderComponent();

    const taskCard = await screen.findByText('First Test Task');
    expect(taskCard).toBeInTheDocument();

    // Drag start on issue
    fireEvent.dragStart(taskCard);

    // Drop onto IN_PROGRESS column
    const inProgressHeader = screen.getByText('In Progress');
    const inProgressColumn = inProgressHeader.closest('div[class*="min-h-"]');
    expect(inProgressColumn).not.toBeNull();

    fireEvent.drop(inProgressColumn!);

    expect(issuesApi.updateStatus).toHaveBeenCalledWith(101, 'IN_PROGRESS');

    // Error toast called with server failure message
    await waitFor(() => {
      expect(mockToastError).toHaveBeenCalledWith('Concurrent update conflict');
    });

    // The issue remains rendered back in the board
    expect(screen.getByText('First Test Task')).toBeInTheDocument();
  });
});
