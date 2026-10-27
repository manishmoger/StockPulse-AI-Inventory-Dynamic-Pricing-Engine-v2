import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, TrendingUp, Package, Sparkles, RefreshCw } from 'lucide-react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { getDashboardMetrics } from '../api';
import type { DashboardMetrics } from '../types';
import { StatCard, Card, Spinner, EmptyState } from '../components/ui';
import { formatDate } from '../utils';
import { usePolling } from '../hooks/usePolling';

const PIE_COLORS = ['#10b981', '#f59e0b', '#ef4444'];

const activityIcon: Record<string, string> = {
  PRODUCT_CREATED: '📦',
  STOCK_UPDATED: '📊',
  ORDER_SIMULATED: '🛒',
  PRICING_RECOMMENDATION_GENERATED: '💰',
  REORDER_RECOMMENDATION_GENERATED: '🔄',
  RECOMMENDATION_ACCEPTED: '✅',
  RECOMMENDATION_REJECTED: '❌',
  STRATEGY_CHANGED: '⚙️',
};

export default function OverviewPage() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const load = () => {
    getDashboardMetrics()
      .then((m) => { setMetrics(m); setLoading(false); setError(''); })
      .catch((e) => { setError(e.message); setLoading(false); });
  };

  usePolling(load, 5000);
  useEffect(() => { setLoading(true); }, []);

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;
  if (error) return <div className="text-red-600 text-sm p-4 bg-red-50 rounded-lg">{error}</div>;
  if (!metrics) return null;

  const pieData = [
    { name: 'Active', value: metrics.inventoryStatusDistribution['ACTIVE'] ?? 0 },
    { name: 'Low Stock', value: metrics.inventoryStatusDistribution['LOW_STOCK'] ?? 0 },
    { name: 'Out of Stock', value: metrics.inventoryStatusDistribution['OUT_OF_STOCK'] ?? 0 },
  ];

  const barData = metrics.recentActivity.slice(0, 6).map((a) => ({
    name: a.productSku,
    type: a.type.replace(/_/g, ' '),
  }));

  return (
    <div className="space-y-6">
      {/* Stats row */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <StatCard label="Total Products" value={metrics.totalProducts} />
        <StatCard label="Low Stock" value={metrics.lowStockCount} accent={metrics.lowStockCount > 0} />
        <StatCard label="Out of Stock" value={metrics.outOfStockCount} accent={metrics.outOfStockCount > 0} />
        <StatCard label="Pending Pricing" value={metrics.pendingPricingSuggestions} />
        <StatCard label="Pending Reorder" value={metrics.pendingReorderSuggestions} />
        <StatCard label="Demand Spikes" value={metrics.demandSpikeCount} accent={metrics.demandSpikeCount > 0} />
      </div>

      {/* Alerts */}
      {(metrics.lowStockCount > 0 || metrics.outOfStockCount > 0 || metrics.demandSpikeCount > 0) && (
        <Card className="p-4">
          <div className="flex items-center gap-2 mb-3">
            <AlertTriangle size={15} className="text-amber-500" />
            <span className="text-sm font-semibold">Priority Alerts</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {metrics.lowStockCount > 0 && (
              <button onClick={() => navigate('/inventory')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-700 hover:bg-amber-100 transition-colors">
                <Package size={12} /> {metrics.lowStockCount} products low on stock
              </button>
            )}
            {metrics.outOfStockCount > 0 && (
              <button onClick={() => navigate('/inventory')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 hover:bg-red-100 transition-colors">
                <AlertTriangle size={12} /> {metrics.outOfStockCount} products out of stock
              </button>
            )}
            {metrics.demandSpikeCount > 0 && (
              <button onClick={() => navigate('/recommendations')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-orange-50 border border-orange-200 rounded-lg text-xs text-orange-700 hover:bg-orange-100 transition-colors">
                <TrendingUp size={12} /> {metrics.demandSpikeCount} demand spikes detected
              </button>
            )}
            {(metrics.pendingPricingSuggestions > 0 || metrics.pendingReorderSuggestions > 0) && (
              <button onClick={() => navigate('/recommendations')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-700 hover:bg-blue-100 transition-colors">
                <Sparkles size={12} /> {metrics.pendingPricingSuggestions + metrics.pendingReorderSuggestions} recommendations pending review
              </button>
            )}
          </div>
        </Card>
      )}

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card className="p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold">Inventory Status</h2>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={55} outerRadius={80} paddingAngle={3} dataKey="value">
                {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i]} />)}
              </Pie>
              <Tooltip formatter={(v) => [v, '']} />
            </PieChart>
          </ResponsiveContainer>
          <div className="flex justify-center gap-4 mt-2">
            {pieData.map((d, i) => (
              <div key={d.name} className="flex items-center gap-1.5 text-xs text-text-secondary">
                <div className="w-2.5 h-2.5 rounded-full" style={{ background: PIE_COLORS[i] }} />
                {d.name} ({d.value})
              </div>
            ))}
          </div>
        </Card>

        <Card className="p-5">
          <h2 className="text-sm font-semibold mb-4">Recent Activity Types</h2>
          {barData.length === 0 ? (
            <EmptyState icon={<Activity size={32} />} title="No activity yet" />
          ) : (
            <div className="space-y-2">
              {metrics.recentActivity.slice(0, 5).map((a) => (
                <div key={a.id} className="flex items-start gap-2 text-xs">
                  <span className="text-base leading-none mt-0.5">{activityIcon[a.type] ?? '•'}</span>
                  <div className="flex-1 min-w-0">
                    <span className="font-medium text-text-primary">{a.productName}</span>
                    <span className="text-text-muted ml-1">({a.productSku})</span>
                    <p className="text-text-secondary truncate">{a.details}</p>
                  </div>
                  <span className="text-text-muted flex-shrink-0">{formatDate(a.createdAt)}</span>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* Quick actions */}
      <Card className="p-5">
        <h2 className="text-sm font-semibold mb-3">Quick Actions</h2>
        <div className="flex flex-wrap gap-2">
          <button onClick={() => navigate('/inventory')}
            className="px-4 py-2 bg-accent text-white text-sm rounded-lg hover:bg-accent-light transition-colors">
            Manage Inventory
          </button>
          <button onClick={() => navigate('/recommendations')}
            className="px-4 py-2 bg-white border border-border text-sm rounded-lg hover:bg-stone-50 transition-colors">
            Review Recommendations
          </button>
          <button onClick={load}
            className="flex items-center gap-1.5 px-4 py-2 bg-white border border-border text-sm rounded-lg hover:bg-stone-50 transition-colors">
            <RefreshCw size={13} /> Refresh
          </button>
        </div>
      </Card>
    </div>
  );
}

function Activity({ size }: { size: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>;
}
