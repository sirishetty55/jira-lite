import React from 'react';
import { useAuth } from '../context/AuthContext';
import { User, Shield, Key, Database, Server, ExternalLink, Sparkles } from 'lucide-react';

export const Settings: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Profile & Workspace Settings</h1>
        <p className="text-sm text-slate-500">Manage your profile, system credentials and platform configurations</p>
      </div>

      {/* User Card */}
      <div className="bg-white rounded-3xl border border-slate-200/80 p-8 shadow-xs space-y-6">
        <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
          <User className="w-5 h-5 text-blue-600" /> User Profile
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Full Name
            </label>
            <div className="font-semibold text-slate-800 text-base">
              {user?.firstName} {user?.lastName}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Work Email
            </label>
            <div className="font-semibold text-slate-800 text-base">{user?.email}</div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Role & Permissions
            </label>
            <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
              {user?.role}
            </span>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">
              Account Created
            </label>
            <div className="text-slate-600">
              {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active'}
            </div>
          </div>
        </div>
      </div>

      {/* Architecture & Tech Stack Card */}
      <div className="bg-white rounded-3xl border border-slate-200/80 p-8 shadow-xs space-y-6">
        <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
          <Server className="w-5 h-5 text-indigo-600" /> Technology Stack & Infrastructure
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 flex items-center gap-3">
            <Server className="w-8 h-8 text-blue-600 shrink-0" />
            <div>
              <div className="font-bold text-slate-800">Spring Boot 4.1.1 + Java 21</div>
              <span className="text-slate-500">RESTful monolithic service with layered DTOs</span>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 flex items-center gap-3">
            <Database className="w-8 h-8 text-emerald-600 shrink-0" />
            <div>
              <div className="font-bold text-slate-800">PostgreSQL 16 & H2 DB</div>
              <span className="text-slate-500">Foreign keys, constraints, composite indexes & optimistic locking</span>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 flex items-center gap-3">
            <Sparkles className="w-8 h-8 text-amber-500 shrink-0" />
            <div>
              <div className="font-bold text-slate-800">Redis 7 Distributed Cache</div>
              <span className="text-slate-500">Cacheable projects, issues and dashboard with auto-invalidation</span>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 flex items-center gap-3">
            <Key className="w-8 h-8 text-purple-600 shrink-0" />
            <div>
              <div className="font-bold text-slate-800">JWT Authentication</div>
              <span className="text-slate-500">Stateless bearer token with BCrypt encryption</span>
            </div>
          </div>
        </div>
      </div>

      {/* Developer Resources & API Docs */}
      <div className="bg-white rounded-3xl border border-slate-200/80 p-8 shadow-xs space-y-4">
        <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
          <Shield className="w-5 h-5 text-emerald-600" /> Developer & API Documentation
        </h2>
        <p className="text-xs text-slate-500 leading-relaxed">
          Explore interactive OpenAPI / Swagger specifications and directly execute test requests against all protected and public endpoints.
        </p>

        <div className="pt-2">
          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs shadow-md shadow-blue-500/20 transition-all"
          >
            <span>Open Swagger UI</span>
            <ExternalLink className="w-4 h-4" />
          </a>
        </div>
      </div>
    </div>
  );
};

