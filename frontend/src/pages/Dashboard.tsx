import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '../services/api';
import type { DashboardStats } from '../types';
import {
  FolderKanban,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Flame,
  ArrowRight,
  Plus,
  TrendingUp,
  Tag
} from 'lucide-react';

export const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const data = await dashboardApi.getStats();
        setStats(data);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
          <span className="text-sm text-slate-500 font-medium">Loading metrics...</span>
        </div>
      </div>
    );
  }

  const completionRate =
    stats && stats.totalIssues > 0
      ? Math.round(((stats.resolvedIssues + stats.closedIssues) / stats.totalIssues) * 100)
      : 0;

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 animate-fade-in">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Project Overview</h1>
          <p className="text-sm text-slate-500">Real-time metrics, active tasks and project delivery status</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/projects')}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm shadow-md shadow-blue-500/20 transition-all"
          >
            <Plus className="w-4 h-4" /> New Project
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold uppercase text-slate-400">Total Projects</span>
            <div className="text-3xl font-bold text-slate-900 mt-1">{stats?.totalProjects ?? 0}</div>
            <span className="text-xs text-slate-500 mt-1 block">Active workspaces</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
            <FolderKanban className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold uppercase text-slate-400">In Progress</span>
            <div className="text-3xl font-bold text-indigo-600 mt-1">{stats?.inProgressIssues ?? 0}</div>
            <span className="text-xs text-slate-500 mt-1 block">Active sprint items</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <Clock className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold uppercase text-slate-400">Resolved & Closed</span>
            <div className="text-3xl font-bold text-emerald-600 mt-1">
              {(stats?.resolvedIssues ?? 0) + (stats?.closedIssues ?? 0)}
            </div>
            <span className="text-xs text-emerald-600 font-medium mt-1 flex items-center gap-1">
              <TrendingUp className="w-3.5 h-3.5" /> {completionRate}% completed
            </span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold uppercase text-slate-400">Overdue Issues</span>
            <div className="text-3xl font-bold text-rose-600 mt-1">{stats?.overdueIssues ?? 0}</div>
            <span className="text-xs text-slate-500 mt-1 block">Requires attention</span>
          </div>
          <div className="w-12 h-12 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <AlertTriangle className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Status Breakdown & Assigned to Me */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Status Distribution */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs lg:col-span-1 space-y-4">
          <h2 className="text-base font-bold text-slate-900">Issue Lifecycle Status</h2>
          <div className="space-y-3">
            {[
              { label: 'Open', count: stats?.openIssues ?? 0, color: 'bg-slate-400', text: 'text-slate-600' },
              { label: 'In Progress', count: stats?.inProgressIssues ?? 0, color: 'bg-blue-500', text: 'text-blue-600' },
              { label: 'In Review', count: stats?.inReviewIssues ?? 0, color: 'bg-amber-500', text: 'text-amber-600' },
              { label: 'Resolved', count: stats?.resolvedIssues ?? 0, color: 'bg-emerald-500', text: 'text-emerald-600' },
              { label: 'Closed', count: stats?.closedIssues ?? 0, color: 'bg-slate-700', text: 'text-slate-800' },
            ].map((item) => {
              const pct = stats && stats.totalIssues > 0 ? (item.count / stats.totalIssues) * 100 : 0;
              return (
                <div key={item.label}>
                  <div className="flex items-center justify-between text-xs font-medium mb-1">
                    <span className="text-slate-600">{item.label}</span>
                    <span className={`font-semibold ${item.text}`}>
                      {item.count} ({Math.round(pct)}%)
                    </span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden">
                    <div
                      className={`h-full ${item.color} rounded-full transition-all duration-500`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
            <span className="text-xs text-slate-500">Assigned to you:</span>
            <span className="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full">
              {stats?.myAssignedIssues ?? 0} active
            </span>
          </div>
        </div>

        {/* Recent Projects */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-slate-900">Active Projects</h2>
            <button
              onClick={() => navigate('/projects')}
              className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"
            >
              View all <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="space-y-2">
            {!stats?.recentProjects || stats.recentProjects.length === 0 ? (
              <div className="py-8 text-center text-sm text-slate-400">
                No projects yet. Create your first project to get started!
              </div>
            ) : (
              stats.recentProjects.map((p) => (
                <div
                  key={p.id}
                  onClick={() => navigate(`/projects/${p.id}`)}
                  className="p-4 rounded-xl border border-slate-100 hover:border-blue-200 hover:bg-blue-50/20 cursor-pointer flex items-center justify-between transition-all group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-xs">
                      {p.projectKey}
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-slate-900 group-hover:text-blue-600 transition-colors">
                        {p.name}
                      </h4>
                      <span className="text-xs text-slate-400">Key: {p.projectKey}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-4 text-xs">
                    <div className="text-right">
                      <span className="font-semibold text-slate-700">{p.issueCount}</span>{' '}
                      <span className="text-slate-400">issues</span>
                    </div>
                    <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-medium">
                      {p.openIssueCount} open
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Recent Issues Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-slate-900">Recent Issues & Tasks</h2>
            <p className="text-xs text-slate-500">Latest updates across your active workspace</p>
          </div>
          <button
            onClick={() => navigate('/my-issues')}
            className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"
          >
            My Issues <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-400 font-semibold uppercase border-b border-slate-100">
              <tr>
                <th className="px-6 py-3">Key</th>
                <th className="px-6 py-3">Title</th>
                <th className="px-6 py-3">Type</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Priority</th>
                <th className="px-6 py-3">Assignee</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {!stats?.recentIssues || stats.recentIssues.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-400">
                    No recent issues found.
                  </td>
                </tr>
              ) : (
                stats.recentIssues.map((issue) => (
                  <tr
                    key={issue.id}
                    onClick={() => navigate(`/issues/${issue.id}`)}
                    className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                  >
                    <td className="px-6 py-3.5 font-mono font-semibold text-blue-600">
                      {issue.issueKey}
                    </td>
                    <td className="px-6 py-3.5 font-medium text-slate-800 max-w-xs truncate">
                      {issue.title}
                    </td>
                    <td className="px-6 py-3.5">
                      <span className="inline-flex items-center gap-1 font-semibold text-[11px] text-slate-600">
                        <Tag className="w-3 h-3 text-slate-400" /> {issue.type}
                      </span>
                    </td>
                    <td className="px-6 py-3.5">
                      <span
                        className={`px-2 py-0.5 rounded-full font-semibold text-[10px] ${
                          issue.status === 'RESOLVED' || issue.status === 'CLOSED'
                            ? 'bg-emerald-50 text-emerald-700'
                            : issue.status === 'IN_PROGRESS'
                            ? 'bg-blue-50 text-blue-700'
                            : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {issue.status}
                      </span>
                    </td>
                    <td className="px-6 py-3.5">
                      <span
                        className={`inline-flex items-center gap-1 text-[11px] font-semibold ${
                          issue.priority === 'CRITICAL'
                            ? 'text-rose-600'
                            : issue.priority === 'HIGH'
                            ? 'text-amber-600'
                            : 'text-slate-600'
                        }`}
                      >
                        {issue.priority === 'CRITICAL' && <Flame className="w-3.5 h-3.5" />}
                        {issue.priority}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-slate-600">
                      {issue.assignee ? (
                        <div className="flex items-center gap-1.5">
                          <div className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 font-bold text-[9px] flex items-center justify-center">
                            {issue.assignee.firstName[0]}
                          </div>
                          <span>{issue.assignee.firstName} {issue.assignee.lastName}</span>
                        </div>
                      ) : (
                        <span className="text-slate-400 italic">Unassigned</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

