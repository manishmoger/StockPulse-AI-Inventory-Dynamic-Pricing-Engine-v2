import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Package, Sparkles, Activity, Settings, ChevronLeft, ChevronRight, Zap,
} from 'lucide-react';

const nav = [
  { to: '/',               icon: LayoutDashboard, label: 'Overview' },
  { to: '/inventory',      icon: Package,         label: 'Inventory' },
  { to: '/recommendations',icon: Sparkles,        label: 'AI Recommendations' },
  { to: '/activity',       icon: Activity,        label: 'Activity' },
  { to: '/settings',       icon: Settings,        label: 'Settings' },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const pageTitle = nav.find((n) => n.to === location.pathname)?.label ?? 'StockPulse';

  return (
    <div className="flex h-screen overflow-hidden bg-page">
      {/* Sidebar */}
      <aside className={`flex flex-col bg-surface border-r border-border transition-all duration-200 ${collapsed ? 'w-16' : 'w-56'}`}>
        {/* Brand */}
        <div className="flex items-center gap-2 px-4 py-4 border-b border-border">
          <div className="flex-shrink-0 w-7 h-7 bg-accent rounded-lg flex items-center justify-center">
            <Zap size={14} className="text-white" />
          </div>
          {!collapsed && <span className="font-bold text-sm text-text-primary tracking-tight">StockPulse</span>}
        </div>

        {/* Nav */}
        <nav className="flex-1 py-3 space-y-0.5 px-2">
          {nav.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-accent/10 text-accent font-medium'
                    : 'text-text-secondary hover:bg-stone-50 hover:text-text-primary'
                }`
              }
            >
              <Icon size={16} className="flex-shrink-0" />
              {!collapsed && <span>{label}</span>}
            </NavLink>
          ))}
        </nav>

        {/* Collapse toggle */}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="flex items-center justify-center p-3 border-t border-border text-text-muted hover:text-text-primary transition-colors"
          aria-label="Toggle sidebar"
        >
          {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top bar */}
        <header className="flex items-center justify-between px-6 py-3 bg-surface border-b border-border">
          <h1 className="text-sm font-semibold text-text-primary">{pageTitle}</h1>
          <div className="flex items-center gap-3">
            <span className="text-xs text-text-muted">Demo Mode</span>
            <div className="w-7 h-7 rounded-full bg-accent/10 flex items-center justify-center text-accent text-xs font-bold">S</div>
          </div>
        </header>

        {/* Content */}
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
