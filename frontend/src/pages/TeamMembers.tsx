import React, { useState, useEffect } from 'react';
import { authApi } from '../services/api';
import type { User } from '../types';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import { Mail, Calendar, CheckCircle2, XCircle } from 'lucide-react';

export const TeamMembers: React.FC = () => {
  const [members, setMembers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const { error } = useToast();

  useEffect(() => {
    const fetchTeam = async () => {
      try {
        setLoading(true);
        const res = await authApi.getAllUsers(0, 100);
        setMembers(res.content);
      } catch {
        error('Failed to load team members');
      } finally {
        setLoading(false);
      }
    };
    fetchTeam();
  }, [error]);

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Team Directory</h1>
        <p className="text-sm text-slate-500">All registered engineers, managers and administrators</p>
      </div>

      {loading ? (
        <div className="p-12 flex justify-center">
          <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs divide-y divide-slate-100 overflow-hidden">
          {members.map((member) => (
            <div key={member.id} className="p-5 flex items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-bold text-sm flex items-center justify-center shadow-xs">
                  {member.firstName[0]}
                  {member.lastName[0]}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-slate-900 text-sm">
                      {member.firstName} {member.lastName}
                    </h3>
                    {member.id === user?.id && (
                      <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-blue-100 text-blue-700">
                        You
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-4 text-xs text-slate-400 mt-1">
                    <span className="flex items-center gap-1">
                      <Mail className="w-3.5 h-3.5" /> {member.email}
                    </span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5" /> Joined {new Date(member.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-4">
                <span
                  className={`px-3 py-1 rounded-full text-xs font-semibold ${
                    member.role === 'ADMIN'
                      ? 'bg-purple-50 text-purple-700 border border-purple-200'
                      : member.role === 'PROJECT_MANAGER'
                      ? 'bg-blue-50 text-blue-700 border border-blue-200'
                      : 'bg-slate-100 text-slate-700 border border-slate-200'
                  }`}
                >
                  {member.role.replace('_', ' ')}
                </span>

                <div className="flex items-center gap-1 text-xs">
                  {member.active ? (
                    <span className="flex items-center gap-1 text-emerald-600 font-medium">
                      <CheckCircle2 className="w-4 h-4" /> Active
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-rose-500 font-medium">
                      <XCircle className="w-4 h-4" /> Inactive
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
