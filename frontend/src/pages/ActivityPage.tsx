import { useEffect, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { getActivity } from '../api';
import type { ActivityLog } from '../types';
import { Card, Button, Spinner, EmptyState } from '../components/ui';
import { formatDate } from '../utils';

const activityMeta: Record<string, { icon: string; color: string }> = {
  PRODUCT_CREATED:                    { icon: '📦', color: 'bg-blue-50 text-blue-700' },
  STOCK_UPDATED:                      { icon: '📊', color: 'bg-stone-50 text-stone-600' },
  ORDER_SIMULATED:                    { icon: '🛒', color: 'bg-purple-50 text-purple-700' },
  PRICING_RECOMMENDATION_GENERATED:   { icon: '💰', color: 'bg-amber-50 text-amber-700' },
  REORDER_RECOMMENDATION_GENERATED:   { icon: '🔄', color: 'bg-cyan-50 text-cyan-700' },
  RECOMMENDATION_ACCEPTED:            { icon: '✅', color: 'bg-emerald-50 text-emerald-700' },
  RECOMMENDATION_REJECTED:            { icon: '❌', color: 'bg-red-50 text-red-700' },
  STRATEGY_CHANGED:                   { icon: '⚙️', color: 'bg-orange-50 text-orange-700' },
};

export default function ActivityPage() {
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    getActivity()
      .then(setLogs)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-text-secondary">{logs.length} events recorded</p>
        <Button size="sm" variant="ghost" onClick={load}><RefreshCw size={13} /></Button>
      </div>

      <Card>
        {loading ? (
          <div className="flex justify-center py-16"><Spinner size="lg" /></div>
        ) : error ? (
          <div className="p-4 text-sm text-red-600">{error}</div>
        ) : logs.length === 0 ? (
          <EmptyState icon={<ActivityIcon size={32} />} title="No activity yet" description="Events will appear here as you use the application." />
        ) : (
          <div className="divide-y divide-border">
            {logs.map((log) => {
              const meta = activityMeta[log.type] ?? { icon: '•', color: 'bg-stone-50 text-stone-600' };
              return (
                <div key={log.id} className="flex items-start gap-3 px-5 py-3.5 hover:bg-stone-50 transition-colors">
                  <span className="text-lg leading-none mt-0.5 flex-shrink-0">{meta.icon}</span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm font-medium text-text-primary">{log.productName}</span>
                      <span className="text-xs font-mono text-text-muted">{log.productSku}</span>
                      <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${meta.color}`}>
                        {log.type.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <p className="text-xs text-text-secondary mt-0.5 truncate">{log.details}</p>
                  </div>
                  <span className="text-xs text-text-muted flex-shrink-0">{formatDate(log.createdAt)}</span>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
}

function ActivityIcon({ size }: { size: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>;
}
