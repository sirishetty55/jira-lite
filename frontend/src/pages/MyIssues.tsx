import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { issuesApi } from '../services/api';
import type { Issue } from '../types';
import { CheckSquare, Flame, Calendar, ArrowRight, Tag } from 'lucide-react';

export const MyIssues: React.FC = () => {
  const [issues, setIssues] = useState<Issue[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'RESOLVED'>('ACTIVE');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchMyIssues = async () => {
      try {
        setLoading(true);
        const res = await issuesApi.getMyIssues(0, 100);
        setIssues(res.content);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchMyIssues();
  }, []);

  const filtered = issues.filter((i) => {
    if (activeFilter === 'ACTIVE') {
      return i.status !== 'RESOLVED' && i.status !== 'CLOSED';
    }
    if (activeFilter === 'RESOLVED') {
      return i.status === 'RESOLVED' || i.status === 'CLOSED';
    }
    return true;
  });

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Assigned to Me</h1>
          <p className="text-sm text-slate-500">Your personal sprint queue and open deliverables</p>
        </div>

        {/* Filter pills */}
        <div className="flex items-center gap-1.5 bg-slate-200/60 p-1 rounded-xl text-xs font-semibold">
          <button
            onClick={() => setActiveFilter('ACTIVE')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              activeFilter === 'ACTIVE'
                ? 'bg-white text-blue-600 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Active Tasks ({issues.filter((i) => i.status !== 'RESOLVED' && i.status !== 'CLOSED').length})
          </button>
          <button
            onClick={() => setActiveFilter('RESOLVED')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              activeFilter === 'RESOLVED'
                ? 'bg-white text-blue-600 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Completed ({issues.filter((i) => i.status === 'RESOLVED' || i.status === 'CLOSED').length})
          </button>
          <button
            onClick={() => setActiveFilter('ALL')}
            className={`px-3 py-1.5 rounded-lg transition-colors ${
              activeFilter === 'ALL'
                ? 'bg-white text-blue-600 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            All ({issues.length})
          </button>
        </div>
      </div>

      {loading ? (
        <div className="p-12 flex justify-center">
          <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-2xl border border-dashed border-slate-300 p-12 text-center">
          <CheckSquare className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="font-semibold text-slate-700">No issues assigned to you</h3>
          <p className="text-xs text-slate-400 mt-1">
            {activeFilter === 'ACTIVE'
              ? 'Great work! You have cleared your assigned backlog.'
              : 'Tasks assigned to you will show up here.'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs divide-y divide-slate-100 overflow-hidden">
          {filtered.map((issue) => (
            <div
              key={issue.id}
              onClick={() => navigate(`/issues/${issue.id}`)}
              className="p-5 hover:bg-slate-50/70 cursor-pointer flex items-center justify-between gap-4 transition-colors group"
            >
              <div className="flex items-center gap-4">
                <span className="font-mono font-bold text-xs px-2.5 py-1 rounded bg-blue-50 text-blue-700 border border-blue-200 shrink-0">
                  {issue.issueKey}
                </span>
                <div>
                  <h4 className="text-sm font-semibold text-slate-900 group-hover:text-blue-600 transition-colors">
                    {issue.title}
                  </h4>
                  <div className="flex items-center gap-3 text-xs text-slate-400 mt-1">
                    <span className="flex items-center gap-1">
                      <Tag className="w-3 h-3 text-slate-400" /> {issue.type}
                    </span>
                    <span>•</span>
                    <span>Project: {issue.projectKey}</span>
                    {issue.dueDate && (
                      <>
                        <span>•</span>
                        <span className="flex items-center gap-1 text-slate-500">
                          <Calendar className="w-3 h-3" />
                          Due {new Date(issue.dueDate).toLocaleDateString()}
                        </span>
                      </>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-4">
                <span
                  className={`inline-flex items-center gap-1 text-xs font-semibold ${
                    issue.priority === 'CRITICAL'
                      ? 'text-rose-600'
                      : issue.priority === 'HIGH'
                      ? 'text-amber-600'
                      : 'text-slate-500'
                  }`}
                >
                  {issue.priority === 'CRITICAL' && <Flame className="w-3.5 h-3.5" />}
                  {issue.priority}
                </span>

                <span
                  className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                    issue.status === 'RESOLVED' || issue.status === 'CLOSED'
                      ? 'bg-emerald-50 text-emerald-700'
                      : issue.status === 'IN_PROGRESS'
                      ? 'bg-blue-50 text-blue-700'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {issue.status}
                </span>

                <ArrowRight className="w-4 h-4 text-slate-300 group-hover:text-blue-600 group-hover:translate-x-1 transition-all" />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

