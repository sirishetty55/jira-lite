import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FolderKanban,
  CheckSquare,
  Users,
  Settings,
  Layers,
  Sparkles
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/projects', label: 'Projects', icon: FolderKanban },
    { to: '/my-issues', label: 'My Issues', icon: CheckSquare },
    { to: '/team', label: 'Team', icon: Users },
    { to: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <aside className="w-64 border-r border-slate-200 bg-white flex flex-col justify-between shrink-0 h-screen sticky top-0">
      <div>
        {/* Logo */}
        <div className="h-16 flex items-center px-6 gap-3 border-b border-slate-100">
          <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <span className="font-bold text-base tracking-tight text-slate-900 flex items-center gap-1.5">
              Jira-Lite <span className="text-[10px] px-1.5 py-0.2 bg-blue-50 text-blue-600 rounded font-semibold border border-blue-200">v1.0</span>
            </span>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="p-4 space-y-1">
          <div className="px-3 py-1.5 text-[11px] font-semibold tracking-wider uppercase text-slate-400">
            Workspace
          </div>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-700 font-semibold shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`
              }
            >
              <item.icon className="w-4 h-4 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Footer Banner */}
      <div className="p-4 m-4 rounded-xl bg-gradient-to-br from-slate-900 to-indigo-950 text-white shadow-lg">
        <div className="flex items-center gap-2 mb-1.5">
          <Sparkles className="w-4 h-4 text-amber-400" />
          <span className="text-xs font-semibold">Production Ready</span>
        </div>
        <p className="text-[11px] text-slate-300 leading-relaxed">
          Spring Boot 4.1.1 + React 19 + PostgreSQL + Redis
        </p>
      </div>
    </aside>
  );
};

