import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { issuesApi, commentsApi, authApi } from '../services/api';
import type {
  IssueDetail as IssueDetailType,
  Comment,
  ActivityLog,
  IssueStatus,
  Priority,
  User,
} from '../types';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import {
  ArrowLeft,
  MessageSquare,
  Activity,
  Send,
  Trash2,
  Edit2,
  Check,
  X,
} from 'lucide-react';

export const IssueDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const issueId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();
  const { success, error } = useToast();

  const [issue, setIssue] = useState<IssueDetailType | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [activity, setActivity] = useState<ActivityLog[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  // New comment input
  const [commentText, setCommentText] = useState('');
  const [postingComment, setPostingComment] = useState(false);

  // Editing comment
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editCommentText, setEditCommentText] = useState('');

  // Editing issue title & description
  const [isEditingDetails, setIsEditingDetails] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [savingDetails, setSavingDetails] = useState(false);

  const fetchIssueData = useCallback(async () => {
    try {
      setLoading(true);
      const [iss, comPage, actPage] = await Promise.all([
        issuesApi.getById(issueId),
        commentsApi.getByIssue(issueId, 0, 50),
        issuesApi.getActivity(issueId),
      ]);
      setIssue(iss);
      setComments(comPage.content);
      setActivity(actPage.content);
      setEditTitle(iss.title);
      setEditDescription(iss.description || '');
    } catch {
      error('Failed to load issue');
    } finally {
      setLoading(false);
    }
  }, [issueId, error]);

  useEffect(() => {
    if (issueId) {
      fetchIssueData();
      authApi.getAllUsers(0, 100).then((res) => setAllUsers(res.content)).catch(() => {});
    }
  }, [issueId, fetchIssueData]);

  // Status transition
  const handleStatusChange = async (newStatus: IssueStatus) => {
    if (!issue) return;
    try {
      await issuesApi.updateStatus(issueId, newStatus);
      setIssue((prev) => (prev ? { ...prev, status: newStatus } : null));
      success(`Status updated to ${newStatus}`);
      issuesApi.getActivity(issueId).then((res) => setActivity(res.content));
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 409) {
        error('This issue was updated concurrently by another user. Reloading...');
        fetchIssueData();
      } else {
        error(axiosErr.response?.data?.message || 'Failed to update status');
      }
    }
  };

  // Assignee change
  const handleAssigneeChange = async (userId: number | null) => {
    if (!issue) return;
    try {
      await issuesApi.assign(issueId, userId);
      const assignedUser = allUsers.find((u) => u.id === userId) || null;
      setIssue((prev) => (prev ? { ...prev, assignee: assignedUser } : null));
      success(assignedUser ? `Assigned to ${assignedUser.firstName}` : 'Issue unassigned');
      issuesApi.getActivity(issueId).then((res) => setActivity(res.content));
    } catch {
      error('Failed to change assignee');
    }
  };

  // Priority change
  const handlePriorityChange = async (newPriority: Priority) => {
    if (!issue) return;
    try {
      await issuesApi.update(issueId, {
        title: issue.title,
        description: issue.description,
        type: issue.type,
        priority: newPriority,
        assigneeId: issue.assignee?.id || null,
        dueDate: issue.dueDate || null,
      });
      setIssue((prev) => (prev ? { ...prev, priority: newPriority } : null));
      success(`Priority changed to ${newPriority}`);
    } catch {
      error('Failed to update priority');
    }
  };

  // Save title and description
  const handleSaveDetails = async () => {
    if (!issue || !editTitle.trim()) return;
    setSavingDetails(true);
    try {
      await issuesApi.update(issueId, {
        title: editTitle,
        description: editDescription,
        type: issue.type,
        priority: issue.priority,
        assigneeId: issue.assignee?.id || null,
        dueDate: issue.dueDate || null,
      });
      setIssue((prev) => (prev ? { ...prev, title: editTitle, description: editDescription } : null));
      setIsEditingDetails(false);
      success('Issue details saved');
      issuesApi.getActivity(issueId).then((res) => setActivity(res.content));
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 409) {
        error('This issue was modified by another user. Reloading...');
        fetchIssueData();
      } else {
        error('Failed to save changes');
      }
    } finally {
      setSavingDetails(false);
    }
  };

  // Add Comment
  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    setPostingComment(true);
    try {
      const newComment = await commentsApi.add(issueId, commentText);
      setComments((prev) => [newComment, ...prev]);
      setCommentText('');
      success('Comment posted');
      issuesApi.getActivity(issueId).then((res) => setActivity(res.content));
    } catch {
      error('Failed to post comment');
    } finally {
      setPostingComment(false);
    }
  };

  // Update Comment
  const handleUpdateComment = async (commentId: number) => {
    if (!editCommentText.trim()) return;
    try {
      const updated = await commentsApi.update(issueId, commentId, editCommentText);
      setComments((prev) => prev.map((c) => (c.id === commentId ? updated : c)));
      setEditingCommentId(null);
      success('Comment updated');
    } catch {
      error('Failed to update comment');
    }
  };

  // Delete Comment
  const handleDeleteComment = async (commentId: number) => {
    if (!window.confirm('Delete this comment?')) return;
    try {
      await commentsApi.delete(issueId, commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      success('Comment deleted');
    } catch {
      error('Failed to delete comment');
    }
  };

  // Delete Issue
  const handleDeleteIssue = async () => {
    if (!window.confirm('Are you sure you want to permanently delete this issue?')) return;
    try {
      await issuesApi.delete(issueId);
      success('Issue deleted');
      navigate(`/projects/${issue?.projectId}`);
    } catch {
      error('Failed to delete issue');
    }
  };

  if (loading || !issue) {
    return (
      <div className="p-12 flex justify-center">
        <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-6 animate-fade-in">
      {/* Back button */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate(`/projects/${issue.projectId}`)}
          className="flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to {issue.projectName}
        </button>
        <button
          onClick={handleDeleteIssue}
          className="flex items-center gap-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 px-3 py-1.5 rounded-lg border border-rose-200 transition-colors"
        >
          <Trash2 className="w-3.5 h-3.5" /> Delete Issue
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Title, Description, Comments */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-xs space-y-4">
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-xs px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                {issue.issueKey}
              </span>
              <span className="text-xs font-medium text-slate-400">
                Created on {new Date(issue.createdAt).toLocaleDateString()} by{' '}
                <strong className="text-slate-700">
                  {issue.reporter.firstName} {issue.reporter.lastName}
                </strong>
              </span>
            </div>

            {/* Editable Title */}
            {isEditingDetails ? (
              <div className="space-y-3">
                <input
                  type="text"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full text-xl font-bold px-3 py-1.5 rounded-xl border border-blue-400 focus:outline-hidden"
                />
                <textarea
                  rows={6}
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  placeholder="Issue description..."
                  className="w-full text-sm px-3 py-2 rounded-xl border border-blue-400 focus:outline-hidden"
                />
                <div className="flex gap-2 justify-end">
                  <button
                    onClick={() => {
                      setIsEditingDetails(false);
                      setEditTitle(issue.title);
                      setEditDescription(issue.description || '');
                    }}
                    className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-100"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleSaveDetails}
                    disabled={savingDetails}
                    className="px-4 py-1.5 rounded-lg text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50"
                  >
                    {savingDetails ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </div>
            ) : (
              <div>
                <div className="flex items-start justify-between gap-4">
                  <h1 className="text-xl font-bold text-slate-900 leading-snug">{issue.title}</h1>
                  <button
                    onClick={() => setIsEditingDetails(true)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50"
                    title="Edit issue"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="mt-4 pt-4 border-t border-slate-100">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                    Description
                  </h3>
                  <div className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed">
                    {issue.description || <span className="italic text-slate-400">No description provided.</span>}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Comments Section */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-xs space-y-6">
            <div className="flex items-center gap-2">
              <MessageSquare className="w-5 h-5 text-blue-600" />
              <h3 className="text-base font-bold text-slate-900">Discussion ({comments.length})</h3>
            </div>

            {/* Post comment box */}
            <form onSubmit={handleAddComment} className="space-y-3">
              <textarea
                rows={3}
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                placeholder="Leave a comment, suggestion, or status update..."
                className="w-full text-sm px-4 py-3 rounded-xl border border-slate-200 focus:outline-hidden focus:border-blue-500 transition-all"
              />
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={postingComment || !commentText.trim()}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold shadow-xs disabled:opacity-50 transition-all"
                >
                  <Send className="w-3.5 h-3.5" />
                  {postingComment ? 'Posting...' : 'Post Comment'}
                </button>
              </div>
            </form>

            {/* Comments list */}
            <div className="divide-y divide-slate-100 space-y-4">
              {comments.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400">
                  No comments yet. Start the conversation!
                </div>
              ) : (
                comments.map((c) => {
                  const isAuthor = c.author.id === user?.id;
                  const canDelete = isAuthor || user?.role === 'ADMIN';

                  return (
                    <div key={c.id} className="pt-4 first:pt-0 space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2.5">
                          <div className="w-7 h-7 rounded-full bg-slate-200 text-slate-700 font-bold text-xs flex items-center justify-center">
                            {c.author.firstName[0]}
                          </div>
                          <div>
                            <span className="text-xs font-bold text-slate-900">
                              {c.author.firstName} {c.author.lastName}
                            </span>
                            <span className="text-[10px] text-slate-400 ml-2">
                              {new Date(c.createdAt).toLocaleString()}
                            </span>
                          </div>
                        </div>

                        {canDelete && (
                          <div className="flex items-center gap-1">
                            {isAuthor && editingCommentId !== c.id && (
                              <button
                                onClick={() => {
                                  setEditingCommentId(c.id);
                                  setEditCommentText(c.content);
                                }}
                                className="p-1 rounded text-slate-400 hover:text-blue-600 hover:bg-blue-50"
                              >
                                <Edit2 className="w-3.5 h-3.5" />
                              </button>
                            )}
                            <button
                              onClick={() => handleDeleteComment(c.id)}
                              className="p-1 rounded text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        )}
                      </div>

                      {editingCommentId === c.id ? (
                        <div className="space-y-2">
                          <textarea
                            rows={2}
                            value={editCommentText}
                            onChange={(e) => setEditCommentText(e.target.value)}
                            className="w-full text-xs p-2 border border-blue-400 rounded-lg focus:outline-hidden"
                          />
                          <div className="flex justify-end gap-1.5">
                            <button
                              onClick={() => setEditingCommentId(null)}
                              className="px-2.5 py-1 text-xs rounded text-slate-600 hover:bg-slate-100"
                            >
                              <X className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() => handleUpdateComment(c.id)}
                              className="px-2.5 py-1 text-xs rounded bg-blue-600 text-white font-semibold flex items-center gap-1"
                            >
                              <Check className="w-3.5 h-3.5" /> Save
                            </button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-xs text-slate-700 whitespace-pre-wrap leading-relaxed pl-9">
                          {c.content}
                        </p>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Attributes & Audit History */}
        <div className="space-y-6">
          {/* Metadata Card */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-xs space-y-4">
            <h3 className="font-bold text-slate-900 text-sm border-b border-slate-100 pb-3">
              Properties
            </h3>

            <div>
              <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
                Status
              </label>
              <select
                value={issue.status}
                onChange={(e) => handleStatusChange(e.target.value as IssueStatus)}
                className="w-full text-xs font-semibold px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 focus:outline-hidden focus:border-blue-500"
              >
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="IN_REVIEW">In Review</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
                Assignee
              </label>
              <select
                value={issue.assignee?.id || ''}
                onChange={(e) => handleAssigneeChange(e.target.value ? Number(e.target.value) : null)}
                className="w-full text-xs px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 focus:outline-hidden focus:border-blue-500"
              >
                <option value="">Unassigned</option>
                {allUsers.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.firstName} {u.lastName} ({u.email})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-semibold uppercase text-slate-400 mb-1">
                Priority
              </label>
              <select
                value={issue.priority}
                onChange={(e) => handlePriorityChange(e.target.value as Priority)}
                className="w-full text-xs px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 focus:outline-hidden focus:border-blue-500"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <div className="pt-2 border-t border-slate-100 space-y-2.5 text-xs">
              <div className="flex justify-between items-center text-slate-500">
                <span>Issue Type</span>
                <span className="font-semibold text-slate-800 px-2 py-0.5 rounded bg-slate-100 text-[10px]">
                  {issue.type}
                </span>
              </div>
              <div className="flex justify-between items-center text-slate-500">
                <span>Due Date</span>
                <span className="font-medium text-slate-800">
                  {issue.dueDate ? new Date(issue.dueDate).toLocaleDateString() : 'None'}
                </span>
              </div>
              <div className="flex justify-between items-center text-slate-500">
                <span>Optimistic Version</span>
                <span className="font-mono text-slate-600">v{issue.version ?? 0}</span>
              </div>
            </div>
          </div>

          {/* Audit History Card */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-xs space-y-4">
            <div className="flex items-center gap-2">
              <Activity className="w-4 h-4 text-blue-600" />
              <h3 className="font-bold text-slate-900 text-sm">Issue Activity</h3>
            </div>

            <div className="space-y-3 max-h-72 overflow-y-auto">
              {activity.length === 0 ? (
                <div className="text-xs text-slate-400 text-center py-4">No activity yet</div>
              ) : (
                activity.map((log) => (
                  <div key={log.id} className="text-xs pb-2 border-b border-slate-50 last:border-none">
                    <span className="font-semibold text-slate-800">{log.user?.firstName}:</span>{' '}
                    <span className="text-slate-600">{log.details}</span>
                    <div className="text-[10px] text-slate-400 mt-0.5">
                      {new Date(log.createdAt).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
