import React from 'react';
import type { TriggerReason, SuggestionStatus, ProductStatus, RecommendationSource } from '../types';

// ── Badge ──────────────────────────────────────────────────────────────────
type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'info' | 'orange';
const badgeStyles: Record<BadgeVariant, string> = {
  default: 'bg-stone-100 text-stone-600',
  success: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  warning: 'bg-amber-50 text-amber-700 border border-amber-200',
  danger:  'bg-red-50 text-red-700 border border-red-200',
  info:    'bg-blue-50 text-blue-700 border border-blue-200',
  orange:  'bg-orange-50 text-orange-700 border border-orange-200',
};

export function Badge({ children, variant = 'default', className = '' }: {
  children: React.ReactNode; variant?: BadgeVariant; className?: string;
}) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${badgeStyles[variant]} ${className}`}>
      {children}
    </span>
  );
}

export function TriggerBadge({ trigger }: { trigger: TriggerReason }) {
  const map: Record<TriggerReason, { label: string; variant: BadgeVariant }> = {
    INVENTORY_LOW: { label: 'Inventory Low', variant: 'danger' },
    DEMAND_SPIKE:  { label: 'Demand Spike',  variant: 'orange' },
    MANUAL:        { label: 'Manual',        variant: 'info' },
    INITIAL:       { label: 'Initial',       variant: 'default' },
  };
  const { label, variant } = map[trigger];
  return <Badge variant={variant}>{label}</Badge>;
}

export function StatusBadge({ status }: { status: SuggestionStatus }) {
  const map: Record<SuggestionStatus, { label: string; variant: BadgeVariant }> = {
    PENDING:  { label: 'Pending',  variant: 'warning' },
    ACCEPTED: { label: 'Accepted', variant: 'success' },
    REJECTED: { label: 'Rejected', variant: 'danger' },
  };
  const { label, variant } = map[status];
  return <Badge variant={variant}>{label}</Badge>;
}

export function ProductStatusBadge({ status }: { status: ProductStatus }) {
  const map: Record<ProductStatus, { label: string; variant: BadgeVariant }> = {
    ACTIVE:               { label: 'Active',         variant: 'success' },
    PRICE_REVIEW_PENDING: { label: 'Review Pending', variant: 'warning' },
    OUT_OF_STOCK:         { label: 'Out of Stock',   variant: 'danger' },
  };
  const { label, variant } = map[status];
  return <Badge variant={variant}>{label}</Badge>;
}

export function SourceBadge({ source }: { source: RecommendationSource }) {
  return (
    <Badge variant={source === 'AI' ? 'info' : 'default'}>
      {source === 'AI' ? '✦ AI' : 'Rule-Based'}
    </Badge>
  );
}

// ── Button ─────────────────────────────────────────────────────────────────
type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'success';
const btnStyles: Record<ButtonVariant, string> = {
  primary:   'bg-accent text-white hover:bg-accent-light',
  secondary: 'bg-white border border-border text-text-primary hover:bg-stone-50',
  danger:    'bg-red-600 text-white hover:bg-red-700',
  ghost:     'text-text-secondary hover:text-text-primary hover:bg-stone-100',
  success:   'bg-emerald-600 text-white hover:bg-emerald-700',
};

export function Button({ children, variant = 'secondary', className = '', disabled, onClick, type = 'button', size = 'md' }: {
  children: React.ReactNode;
  variant?: ButtonVariant;
  className?: string;
  disabled?: boolean;
  onClick?: () => void;
  type?: 'button' | 'submit';
  size?: 'sm' | 'md';
}) {
  const sizeClass = size === 'sm' ? 'px-3 py-1.5 text-xs' : 'px-4 py-2 text-sm';
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`inline-flex items-center gap-1.5 font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${sizeClass} ${btnStyles[variant]} ${className}`}
    >
      {children}
    </button>
  );
}

// ── Card ───────────────────────────────────────────────────────────────────
export function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`bg-surface border border-border rounded-xl shadow-sm ${className}`}>
      {children}
    </div>
  );
}

// ── Spinner ────────────────────────────────────────────────────────────────
export function Spinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const s = size === 'sm' ? 'h-4 w-4' : size === 'lg' ? 'h-8 w-8' : 'h-6 w-6';
  return <div className={`${s} animate-spin rounded-full border-2 border-border border-t-accent`} />;
}

// ── Empty State ────────────────────────────────────────────────────────────
export function EmptyState({ icon, title, description }: { icon: React.ReactNode; title: string; description?: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="text-stone-300 mb-3">{icon}</div>
      <p className="text-sm font-medium text-text-primary">{title}</p>
      {description && <p className="text-xs text-text-muted mt-1 max-w-xs">{description}</p>}
    </div>
  );
}

// ── Stat Card ──────────────────────────────────────────────────────────────
export function StatCard({ label, value, sub, accent }: { label: string; value: string | number; sub?: string; accent?: boolean }) {
  return (
    <Card className="p-5">
      <p className="text-xs font-medium text-text-secondary uppercase tracking-wide">{label}</p>
      <p className={`text-3xl font-bold mt-1 ${accent ? 'text-accent' : 'text-text-primary'}`}>{value}</p>
      {sub && <p className="text-xs text-text-muted mt-1">{sub}</p>}
    </Card>
  );
}

// ── Confidence Bar ─────────────────────────────────────────────────────────
export function ConfidenceBar({ value }: { value: number }) {
  const pct = Math.round(value * 100);
  const color = pct >= 80 ? 'bg-emerald-500' : pct >= 60 ? 'bg-amber-500' : 'bg-red-400';
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-1.5 bg-stone-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-text-secondary w-8 text-right">{pct}%</span>
    </div>
  );
}

// ── Toast ──────────────────────────────────────────────────────────────────
export function Toast({ message, type, onClose }: { message: string; type: 'success' | 'error'; onClose: () => void }) {
  const bg = type === 'success' ? 'bg-emerald-600' : 'bg-red-600';
  return (
    <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-lg text-white text-sm shadow-lg ${bg}`}>
      <span>{message}</span>
      <button onClick={onClose} className="ml-2 opacity-70 hover:opacity-100">✕</button>
    </div>
  );
}

// ── Stock Level Bar ────────────────────────────────────────────────────────
export function StockBar({ stock, threshold }: { stock: number; threshold: number }) {
  const max = Math.max(threshold * 2, stock, 1);
  const pct = Math.min((stock / max) * 100, 100);
  const color = stock === 0 ? 'bg-red-500' : stock < threshold ? 'bg-amber-500' : 'bg-emerald-500';
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-1.5 bg-stone-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-text-secondary w-8 text-right">{stock}</span>
    </div>
  );
}
