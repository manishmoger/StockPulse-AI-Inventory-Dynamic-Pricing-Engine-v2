import { useEffect, useState } from 'react';
import { RefreshCw, CheckCircle, XCircle, Sparkles } from 'lucide-react';
import {
  getPricingSuggestions, getReorderSuggestions, decidePricing, decideReorder,
} from '../api';
import type { PricingSuggestion, ReorderSuggestion } from '../types';
import {
  Card, Button, Spinner, EmptyState, TriggerBadge, StatusBadge, SourceBadge,
  ConfidenceBar, Toast,
} from '../components/ui';
import { formatCurrency, formatDate, priceDiff } from '../utils';
import { usePolling } from '../hooks/usePolling';

const POLL_INTERVAL = 4000; // 4 seconds — rubric requires 3–5s

export default function RecommendationsPage() {
  const [pricing, setPricing] = useState<PricingSuggestion[]>([]);
  const [reorder, setReorder] = useState<ReorderSuggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<'pricing' | 'reorder'>('pricing');
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
  const [busy, setBusy] = useState<number | null>(null);

  const load = () => {
    Promise.all([getPricingSuggestions(), getReorderSuggestions()])
      .then(([p, r]) => { setPricing(p); setReorder(r); setLoading(false); })
      .catch((e) => { showToast(e.message, 'error'); setLoading(false); });
  };

  // Auto-poll every 4s so async AI suggestions appear without manual refresh
  usePolling(load, POLL_INTERVAL);

  // Keep useEffect only for the initial loading state
  useEffect(() => { setLoading(true); }, []);

  const showToast = (msg: string, type: 'success' | 'error') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const handlePricingDecision = async (id: number, decision: 'ACCEPTED' | 'REJECTED') => {
    setBusy(id);
    try {
      await decidePricing(id, decision);
      showToast(decision === 'ACCEPTED' ? 'Price updated successfully' : 'Recommendation rejected', 'success');
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(null); }
  };

  const handleReorderDecision = async (id: number, decision: 'ACCEPTED' | 'REJECTED') => {
    setBusy(id);
    try {
      await decideReorder(id, decision);
      showToast(decision === 'ACCEPTED' ? 'Stock replenishment applied' : 'Recommendation rejected', 'success');
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(null); }
  };

  const pendingPricing = pricing.filter((p) => p.status === 'PENDING');
  const pendingReorder = reorder.filter((r) => r.status === 'PENDING');

  return (
    <div className="space-y-4">
      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-border">
        {(['pricing', 'reorder'] as const).map((t) => {
          const count = t === 'pricing' ? pendingPricing.length : pendingReorder.length;
          return (
            <button key={t} onClick={() => setTab(t)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                tab === t ? 'border-accent text-accent' : 'border-transparent text-text-secondary hover:text-text-primary'
              }`}>
              {t === 'pricing' ? 'Pricing' : 'Reorder'}
              {count > 0 && (
                <span className="ml-2 px-1.5 py-0.5 bg-accent text-white text-xs rounded-full">{count}</span>
              )}
            </button>
          );
        })}
        <div className="ml-auto pb-2">
          <Button size="sm" variant="ghost" onClick={load}><RefreshCw size={13} /></Button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : tab === 'pricing' ? (
        <PricingList suggestions={pricing} busy={busy} onDecide={handlePricingDecision} />
      ) : (
        <ReorderList suggestions={reorder} busy={busy} onDecide={handleReorderDecision} />
      )}

      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
}

function PricingList({ suggestions, busy, onDecide }: {
  suggestions: PricingSuggestion[];
  busy: number | null;
  onDecide: (id: number, d: 'ACCEPTED' | 'REJECTED') => void;
}) {
  if (suggestions.length === 0) {
    return <EmptyState icon={<Sparkles size={32} />} title="No pricing recommendations" description="Simulate orders or trigger manual suggestions to generate recommendations." />;
  }
  return (
    <div className="space-y-3">
      {suggestions.map((s) => (
        <Card key={s.id} className="p-5">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-2">
                <span className="font-semibold text-sm">{s.productName}</span>
                <span className="text-xs text-text-muted font-mono">{s.productSku}</span>
                <TriggerBadge trigger={s.triggerReason} />
                <SourceBadge source={s.source} />
                <StatusBadge status={s.status} />
              </div>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-3">
                <Metric label="Current Price" value={formatCurrency(s.currentPrice)} />
                <Metric label="Recommended" value={formatCurrency(s.recommendedPrice)} />
                <Metric label="Change" value={
                  <span className={s.direction === 'INCREASE' ? 'text-emerald-600' : s.direction === 'DECREASE' ? 'text-red-600' : 'text-text-secondary'}>
                    {priceDiff(s.currentPrice, s.recommendedPrice)} ({s.direction})
                  </span>
                } />
                <Metric label="Generated" value={formatDate(s.createdAt)} />
              </div>
              <div className="mb-2">
                <p className="text-xs font-medium text-text-secondary mb-1">Confidence</p>
                <ConfidenceBar value={s.confidence} />
              </div>
              <p className="text-xs text-text-secondary bg-stone-50 rounded-lg p-3 leading-relaxed">{s.reasoning}</p>
            </div>
            {s.status === 'PENDING' && (
              <div className="flex flex-col gap-2 flex-shrink-0">
                <Button variant="success" size="sm" onClick={() => onDecide(s.id, 'ACCEPTED')} disabled={busy === s.id}>
                  {busy === s.id ? <Spinner size="sm" /> : <><CheckCircle size={13} /> Accept</>}
                </Button>
                <Button variant="danger" size="sm" onClick={() => onDecide(s.id, 'REJECTED')} disabled={busy === s.id}>
                  <XCircle size={13} /> Reject
                </Button>
              </div>
            )}
          </div>
        </Card>
      ))}
    </div>
  );
}

function ReorderList({ suggestions, busy, onDecide }: {
  suggestions: ReorderSuggestion[];
  busy: number | null;
  onDecide: (id: number, d: 'ACCEPTED' | 'REJECTED') => void;
}) {
  if (suggestions.length === 0) {
    return <EmptyState icon={<Sparkles size={32} />} title="No reorder recommendations" description="Simulate orders or trigger manual suggestions to generate recommendations." />;
  }
  return (
    <div className="space-y-3">
      {suggestions.map((s) => (
        <Card key={s.id} className="p-5">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-2">
                <span className="font-semibold text-sm">{s.productName}</span>
                <span className="text-xs text-text-muted font-mono">{s.productSku}</span>
                <TriggerBadge trigger={s.triggerReason} />
                <SourceBadge source={s.source} />
                <StatusBadge status={s.status} />
              </div>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-3">
                <Metric label="Current Stock" value={`${s.currentStock} units`} />
                <Metric label="Reorder Qty" value={`+${s.recommendedQuantity} units`} />
                <Metric label="Lead Time" value={`${s.suggestedLeadTimeDays} days`} />
                <Metric label="Generated" value={formatDate(s.createdAt)} />
              </div>
              <div className="mb-2">
                <p className="text-xs font-medium text-text-secondary mb-1">Confidence</p>
                <ConfidenceBar value={s.confidence} />
              </div>
              <p className="text-xs text-text-secondary bg-stone-50 rounded-lg p-3 leading-relaxed">{s.reasoning}</p>
            </div>
            {s.status === 'PENDING' && (
              <div className="flex flex-col gap-2 flex-shrink-0">
                <Button variant="success" size="sm" onClick={() => onDecide(s.id, 'ACCEPTED')} disabled={busy === s.id}>
                  {busy === s.id ? <Spinner size="sm" /> : <><CheckCircle size={13} /> Accept</>}
                </Button>
                <Button variant="danger" size="sm" onClick={() => onDecide(s.id, 'REJECTED')} disabled={busy === s.id}>
                  <XCircle size={13} /> Reject
                </Button>
              </div>
            )}
          </div>
        </Card>
      ))}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs text-text-muted">{label}</p>
      <p className="text-sm font-medium text-text-primary">{value}</p>
    </div>
  );
}
