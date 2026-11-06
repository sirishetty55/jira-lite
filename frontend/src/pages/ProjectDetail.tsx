import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { projectsApi, issuesApi, authApi } from '../services/api';
import type {
  ProjectDetail as ProjectDetailType,
  Issue,
  IssueStatus,
  IssueType,
  Priority,
  User,
  ActivityLog,
} from '../types';
import { useToast } from '../components/Toast';
import { Modal } from '../components/Modal';
import {
  Kanban,
  ListFilter,
  Users,
  Activity,
  Plus,
  Flame,
  Calendar,
  Trash2,
  UserPlus,
} from 'lucide-react';

const COLUMNS: { id: IssueStatus; label: string; color: string; border: string }[] = [
  { id: 'OPEN', label: 'Open', color: 'bg-slate-100/70', border: 'border-slate-300' },
  { id: 'IN_PROGRESS', label: 'In Progress', color: 'bg-blue-50/70', border: 'border-blue-300' },
  { id: 'IN_REVIEW', label: 'In Review', color: 'bg-amber-50/70', border: 'border-amber-300' },
  { id: 'RESOLVED', label: 'Resolved', color: 'bg-emerald-50/70', border: 'border-emerald-300' },
  { id: 'CLOSED', label: 'Closed', color: 'bg-slate-100/70', border: 'border-slate-300' },
];

export const ProjectDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { success, error } = useToast();

  const [project, setProject] = useState<ProjectDetailType | null>(null);
  const [issues, setIssues] = useState<Issue[]>([]);
  const [activity, setActivity] = useState<ActivityLog[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [activeTab, setActiveTab] = useState<'board' | 'list' | 'members' | 'activity'>('board');
  const [loading, setLoading] = useState(true);

  // Filters for List view
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterPriority, setFilterPriority] = useState<string>('ALL');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [listSearch, setListSearch] = useState<string>('');

  // Create Issue Modal state
  const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newType, setNewType] = useState<IssueType>('TASK');
  const [newPriority, setNewPriority] = useState<Priority>('MEDIUM');
  const [newAssigneeId, setNewAssigneeId] = useState<number | undefined>(undefined);
  const [newDueDate, setNewDueDate] = useState('');
  const [creatingIssue, setCreatingIssue] = useState(false);

  // Add Member Modal state
  const [isMemberModalOpen, setIsMemberModalOpen] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState<number | undefined>(undefined);
  const [addingMember, setAddingMember] = useState(false);

  // Drag-and-drop state
  const [draggedIssueId, setDraggedIssueId] = useState<number | null>(null);

  const fetchProjectData = useCallback(async () => {
    try {
      setLoading(true);
      const [proj, issuePage, actPage] = await Promise.all([
        projectsApi.getById(projectId),
        issuesApi.getByProject(projectId, undefined, undefined, 0, 100),
        projectsApi.getActivity(projectId, 0, 30),
      ]);
      setProject(proj);
      setIssues(issuePage.content);
      setActivity(actPage.content);
    } catch {
      error('Failed to load project');
    } finally {
      setLoading(false);
    }
  }, [projectId, error]);

  useEffect(() => {
    if (projectId) {
      fetchProjectData();
    }
  }, [projectId, fetchProjectData]);

  // Load all users for assignee / member selectors
  useEffect(() => {
    authApi.getAllUsers(0, 100).then((res) => setAllUsers(res.content)).catch(() => {});
  }, []);

  // ── Drag and Drop Handlers ─────────────────────────────────
  const handleDragStart = (issueId: number) => {
    setDraggedIssueId(issueId);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const handleDrop = async (newStatus: IssueStatus) => {
    if (!draggedIssueId) return;
    const targetIssue = issues.find((i) => i.id === draggedIssueId);
    if (!targetIssue || targetIssue.status === newStatus) {
      setDraggedIssueId(null);
      return;
    }

    // Optimistically update board
    setIssues((prev) =>
      prev.map((i) => (i.id === draggedIssueId ? { ...i, status: newStatus } : i))
    );

    try {
      await issuesApi.updateStatus(draggedIssueId, newStatus);
      success(`Moved ${targetIssue.issueKey} to ${newStatus.replace('_', ' ')}`);
      // Refresh activity in background
      projectsApi.getActivity(projectId, 0, 30).then((res) => setActivity(res.content));
    } catch {
      error('Failed to update status');
      fetchProjectData();
    } finally {
      setDraggedIssueId(null);
    }
  };

  // ── Create Issue ───────────────────────────────────────────
  const handleCreateIssue = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) {
      error('Issue title is required');
      return;
    }
    setCreatingIssue(true);
    try {
      const created = await issuesApi.create(projectId, {
        title: newTitle,
        description: newDescription,
        type: newType,
        priority: newPriority,
        assigneeId: newAssigneeId || null,
        dueDate: newDueDate || null,
      });
      success(`Issue ${created.issueKey} created!`);
      setIsIssueModalOpen(false);
      setNewTitle('');
      setNewDescription('');
      setNewType('TASK');
      setNewPriority('MEDIUM');
      setNewAssigneeId(undefined);
      setNewDueDate('');
      fetchProjectData();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      error(axiosErr.response?.data?.message || 'Failed to create issue');
    } finally {
      setCreatingIssue(false);
    }
  };

  // ── Add Member ─────────────────────────────────────────────
  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserId) {
      error('Please select a user');
      return;
    }
    setAddingMember(true);
    try {
      await projectsApi.addMember(projectId, selectedUserId);
      success('Member added to project');
      setIsMemberModalOpen(false);
      setSelectedUserId(undefined);
      fetchProjectData();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      error(axiosErr.response?.data?.message || 'Failed to add member');
    } finally {
      setAddingMember(false);
    }
  };

  // ── Remove Member ──────────────────────────────────────────
  const handleRemoveMember = async (userId: number) => {
    if (!window.confirm('Remove this member from the project?')) return;
    try {
      await projectsApi.removeMember(projectId, userId);
      success('Member removed');
      fetchProjectData();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      error(axiosErr.response?.data?.message || 'Cannot remove member');
    }
  };

  if (loading || !project) {
    return (
      <div className="p-12 flex justify-center">
        <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  // Filtered issues for List tab
  const filteredIssues = issues.filter((i) => {
    if (filterType !== 'ALL' && i.type !== filterType) return false;
    if (filterPriority !== 'ALL' && i.priority !== filterPriority) return false;
    if (filterStatus !== 'ALL' && i.status !== filterStatus) return false;
    if (listSearch) {
      const q = listSearch.toLowerCase();
      return (
        i.title.toLowerCase().includes(q) ||
        i.issueKey.toLowerCase().includes(q) ||
        (i.assignee && `${i.assignee.firstName} ${i.assignee.lastName}`.toLowerCase().includes(q))
      );
    }
    return true;
  });

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6 animate-fade-in">
      {/* Project Header Banner */}
      <div className="bg-white rounded-3xl border border-slate-200/80 p-6 sm:p-8 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white font-bold text-lg flex items-center justify-center shadow-md shadow-blue-500/20 shrink-0">
              {project.projectKey}
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-2xl font-bold text-slate-900">{project.name}</h1>
                <span className="text-xs font-mono font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 border border-slate-200">
                  {project.projectKey}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
                {project.description || 'No description added yet.'}
              </p>
              <div className="flex items-center gap-4 text-xs text-slate-400 mt-3">
                <span>
                  Owner: <strong className="text-slate-700">{project.owner?.firstName} {project.owner?.lastName}</strong>
                </span>
                <span>•</span>
                <span>{project.members?.length || 0} Members</span>
                <span>•</span>
                <span>{issues.length} Issues</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 self-start sm:self-auto">
            <button
              onClick={() => setIsIssueModalOpen(true)}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs shadow-md shadow-blue-500/20 transition-all"
            >
              <Plus className="w-4 h-4" /> Create Issue
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 mt-8 gap-8 text-sm">
          <button
            onClick={() => setActiveTab('board')}
            className={`pb-3 flex items-center gap-2 font-semibold transition-colors border-b-2 -mb-px ${
              activeTab === 'board'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Kanban className="w-4 h-4" /> Kanban Board
          </button>
          <button
            onClick={() => setActiveTab('list')}
            className={`pb-3 flex items-center gap-2 font-semibold transition-colors border-b-2 -mb-px ${
              activeTab === 'list'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <ListFilter className="w-4 h-4" /> List View ({issues.length})
          </button>
          <button
            onClick={() => setActiveTab('members')}
            className={`pb-3 flex items-center gap-2 font-semibold transition-colors border-b-2 -mb-px ${
              activeTab === 'members'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Users className="w-4 h-4" /> Team ({project.members?.length || 0})
          </button>
          <button
            onClick={() => setActiveTab('activity')}
            className={`pb-3 flex items-center gap-2 font-semibold transition-colors border-b-2 -mb-px ${
              activeTab === 'activity'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Activity className="w-4 h-4" /> Activity History
          </button>
        </div>
      </div>

      {/* ── TAB 1: KANBAN BOARD ────────────────────────────────── */}
      {activeTab === 'board' && (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4 items-start">
          {COLUMNS.map((col) => {
            const colIssues = issues.filter((i) => i.status === col.id);
            return (
              <div
                key={col.id}
                onDragOver={handleDragOver}
                onDrop={() => handleDrop(col.id)}
                className={`rounded-2xl p-3 border ${col.border} ${col.color} min-h-[500px] flex flex-col transition-all`}
              >
                <div className="flex items-center justify-between px-2 py-1.5 mb-2">
                  <span className="font-bold text-xs uppercase tracking-wider text-slate-700">
                    {col.label}
                  </span>
                  <span className="w-5 h-5 rounded-full bg-white text-slate-700 font-semibold text-[11px] flex items-center justify-center shadow-xs">
                    {colIssues.length}
                  </span>
                </div>

                <div className="space-y-3 flex-1">
                  {colIssues.length === 0 ? (
                    <div className="h-28 border border-dashed border-slate-300 rounded-xl flex items-center justify-center text-[11px] text-slate-400">
                      Drop here
                    </div>
                  ) : (
                    colIssues.map((issue) => (
                      <div
                        key={issue.id}
                        draggable
                        onDragStart={() => handleDragStart(issue.id)}
                        onClick={() => navigate(`/issues/${issue.id}`)}
                        className="bg-white rounded-xl p-4 border border-slate-200/90 shadow-xs hover:shadow-md hover:border-blue-300 cursor-grab active:cursor-grabbing transition-all space-y-2.5"
                      >
                        <div className="flex items-center justify-between text-[11px]">
                          <span className="font-mono font-semibold text-blue-600">
                            {issue.issueKey}
                          </span>
                          <span
                            className={`inline-flex items-center gap-0.5 font-bold text-[10px] ${
                              issue.priority === 'CRITICAL'
                                ? 'text-rose-600'
                                : issue.priority === 'HIGH'
                                ? 'text-amber-600'
                                : 'text-slate-500'
                            }`}
                          >
                            {issue.priority === 'CRITICAL' && <Flame className="w-3 h-3" />}
                            {issue.priority}
                          </span>
                        </div>

                        <h4 className="text-xs font-semibold text-slate-900 leading-snug line-clamp-2">
                          {issue.title}
                        </h4>

                        <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-[11px] text-slate-400">
                          <span className="px-1.5 py-0.5 rounded bg-slate-100 font-medium text-slate-600 text-[10px]">
                            {issue.type}
                          </span>

                          <div className="flex items-center gap-2">
                            {issue.dueDate && (
                              <span className="flex items-center gap-1 text-[10px] text-slate-500">
                                <Calendar className="w-3 h-3" />
                                {new Date(issue.dueDate).toLocaleDateString([], {
                                  month: 'short',
                                  day: 'numeric',
                                })}
                              </span>
                            )}
                            {issue.assignee ? (
                              <div
                                className="w-5 h-5 rounded-full bg-blue-100 text-blue-700 font-bold text-[9px] flex items-center justify-center shadow-xs"
                                title={`Assigned to ${issue.assignee.firstName} ${issue.assignee.lastName}`}
                              >
                                {issue.assignee.firstName[0]}
                              </div>
                            ) : (
                              <span className="w-2 h-2 rounded-full bg-slate-300" title="Unassigned" />
                            )}
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── TAB 2: LIST VIEW ───────────────────────────────────── */}
      {activeTab === 'list' && (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden space-y-4 p-6">
          {/* Filters Bar */}
          <div className="flex flex-wrap items-center gap-3">
            <input
              type="text"
              placeholder="Search tasks..."
              value={listSearch}
              onChange={(e) => setListSearch(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-hidden focus:border-blue-500 w-60"
            />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg"
            >
              <option value="ALL">All Statuses</option>
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="IN_REVIEW">In Review</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
            <select
              value={filterPriority}
              onChange={(e) => setFilterPriority(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg"
            >
              <option value="ALL">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg"
            >
              <option value="ALL">All Types</option>
              <option value="TASK">Task</option>
              <option value="BUG">Bug</option>
              <option value="STORY">Story</option>
              <option value="EPIC">Epic</option>
            </select>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-400 font-semibold uppercase border-b border-slate-100">
                <tr>
                  <th className="px-4 py-3">Key</th>
                  <th className="px-4 py-3">Title</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Assignee</th>
                  <th className="px-4 py-3">Due Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredIssues.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                      No matching issues found.
                    </td>
                  </tr>
                ) : (
                  filteredIssues.map((issue) => (
                    <tr
                      key={issue.id}
                      onClick={() => navigate(`/issues/${issue.id}`)}
                      className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                    >
                      <td className="px-4 py-3 font-mono font-semibold text-blue-600">
                        {issue.issueKey}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-800 max-w-sm truncate">
                        {issue.title}
                      </td>
                      <td className="px-4 py-3">
                        <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-semibold text-[10px]">
                          {issue.type}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="px-2 py-0.5 rounded-full font-semibold text-[10px] bg-blue-50 text-blue-700">
                          {issue.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-semibold text-slate-700">{issue.priority}</td>
                      <td className="px-4 py-3 text-slate-600">
                        {issue.assignee
                          ? `${issue.assignee.firstName} ${issue.assignee.lastName}`
                          : 'Unassigned'}
                      </td>
                      <td className="px-4 py-3 text-slate-500">{issue.dueDate || '—'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── TAB 3: MEMBERS ────────────────────────────────────── */}
      {activeTab === 'members' && (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs p-6 space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-bold text-slate-900 text-base">Project Members</h3>
              <p className="text-xs text-slate-500">Collaborators who can view and work on project issues</p>
            </div>
            <button
              onClick={() => setIsMemberModalOpen(true)}
              className="flex items-center gap-1.5 px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-xs"
            >
              <UserPlus className="w-3.5 h-3.5" /> Add Member
            </button>
          </div>

          <div className="divide-y divide-slate-100">
            {project.members?.map((member) => (
              <div key={member.id} className="py-3 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-700 font-bold text-xs flex items-center justify-center">
                    {member.user.firstName[0]}
                    {member.user.lastName[0]}
                  </div>
                  <div>
                    <h5 className="font-semibold text-slate-900 text-sm">
                      {member.user.firstName} {member.user.lastName}
                    </h5>
                    <span className="text-xs text-slate-400">{member.user.email}</span>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-xs font-mono font-medium px-2 py-0.5 rounded bg-slate-100 text-slate-600">
                    {member.user.role}
                  </span>
                  {member.user.id !== project.owner?.id && (
                    <button
                      onClick={() => handleRemoveMember(member.user.id)}
                      className="p-1 rounded text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                      title="Remove from project"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── TAB 4: ACTIVITY ───────────────────────────────────── */}
      {activeTab === 'activity' && (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs p-6 space-y-4">
          <h3 className="font-bold text-slate-900 text-base">Audit Trail & Activity Log</h3>
          <div className="space-y-4">
            {activity.length === 0 ? (
              <div className="text-xs text-slate-400 py-6 text-center">No activity recorded yet</div>
            ) : (
              activity.map((log) => (
                <div key={log.id} className="flex items-start gap-3 text-xs">
                  <div className="w-2 h-2 rounded-full bg-blue-500 mt-1.5 shrink-0" />
                  <div className="flex-1">
                    <span className="font-semibold text-slate-800">
                      {log.user?.firstName} {log.user?.lastName}
                    </span>{' '}
                    <span className="text-slate-600">{log.details}</span>
                    <div className="text-[10px] text-slate-400 mt-0.5">
                      {new Date(log.createdAt).toLocaleString()}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* ── MODAL: CREATE ISSUE ───────────────────────────────── */}
      <Modal
        isOpen={isIssueModalOpen}
        onClose={() => setIsIssueModalOpen(false)}
        title="Create New Issue"
        maxWidth="max-w-xl"
      >
        <form onSubmit={handleCreateIssue} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
              Title *
            </label>
            <input
              type="text"
              required
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              placeholder="e.g. Implement OAuth2 Google sign-in flow"
              className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500 focus:outline-hidden"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
                Issue Type
              </label>
              <select
                value={newType}
                onChange={(e) => setNewType(e.target.value as IssueType)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500"
              >
                <option value="TASK">Task</option>
                <option value="BUG">Bug</option>
                <option value="STORY">Story</option>
                <option value="EPIC">Epic</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
                Priority
              </label>
              <select
                value={newPriority}
                onChange={(e) => setNewPriority(e.target.value as Priority)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
                Assignee
              </label>
              <select
                value={newAssigneeId || ''}
                onChange={(e) => setNewAssigneeId(e.target.value ? Number(e.target.value) : undefined)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500"
              >
                <option value="">Unassigned</option>
                {project.members?.map((m) => (
                  <option key={m.user.id} value={m.user.id}>
                    {m.user.firstName} {m.user.lastName} ({m.user.email})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
                Due Date
              </label>
              <input
                type="date"
                value={newDueDate}
                onChange={(e) => setNewDueDate(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
              Description
            </label>
            <textarea
              rows={4}
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              placeholder="Acceptance criteria, steps to reproduce, or technical details..."
              className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500 focus:outline-hidden"
            />
          </div>

          <div className="pt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsIssueModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={creatingIssue}
              className="px-5 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-xs disabled:opacity-50"
            >
              {creatingIssue ? 'Creating...' : 'Create Issue'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ── MODAL: ADD MEMBER ─────────────────────────────────── */}
      <Modal
        isOpen={isMemberModalOpen}
        onClose={() => setIsMemberModalOpen(false)}
        title="Add Member to Project"
      >
        <form onSubmit={handleAddMember} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1.5">
              Select User
            </label>
            <select
              required
              value={selectedUserId || ''}
              onChange={(e) => setSelectedUserId(Number(e.target.value))}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm focus:border-blue-500"
            >
              <option value="">Choose a team member...</option>
              {allUsers
                .filter((u) => !project.members?.some((m) => m.user.id === u.id))
                .map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.firstName} {u.lastName} ({u.email}) — {u.role}
                  </option>
                ))}
            </select>
          </div>

          <div className="pt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsMemberModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={addingMember}
              className="px-5 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-xs disabled:opacity-50"
            >
              {addingMember ? 'Adding...' : 'Add Member'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
