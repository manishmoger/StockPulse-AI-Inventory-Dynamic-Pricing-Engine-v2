import { useEffect, useState } from 'react';
import { getStrategy, setStrategy } from '../api';
import type { StrategyResponse } from '../types';
import { Card, Spinner, Toast } from '../components/ui';
import { CheckCircle, AlertCircle, Cpu, BookOpen } from 'lucide-react';

export default function SettingsPage() {
  const [strategy, setStrategyState] = useState<StrategyResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  const load = () => {
    setLoading(true);
    getStrategy()
      .then(setStrategyState)
      .catch((e) => showToast(e.message, 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const showToast = (msg: string, type: 'success' | 'error') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const switchStrategy = async (name: string) => {
    setBusy(true);
    try {
      const updated = await setStrategy(name);
      setStrategyState(updated);
      showToast(`Strategy switched to ${name}`, 'success');
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;
  if (!strategy) return null;

  const isAi = strategy.activeStrategy === 'AI';
  const isRule = strategy.activeStrategy === 'RULE_BASED';

  return (
    <div className="max-w-2xl space-y-5">
      {/* Active Strategy */}
      <Card className="p-6">
        <h2 className="text-sm font-semibold mb-4">Commerce Strategy</h2>
        <div className="grid grid-cols-2 gap-3">
          {/* Rule-Based */}
          <button
            onClick={() => !isRule && switchStrategy('RULE_BASED')}
            disabled={busy}
            className={`p-4 rounded-xl border-2 text-left transition-all ${
              isRule ? 'border-accent bg-accent/5' : 'border-border hover:border-stone-300'
            }`}
          >
            <div className="flex items-center gap-2 mb-2">
              <BookOpen size={16} className={isRule ? 'text-accent' : 'text-text-secondary'} />
              <span className="text-sm font-medium">Rule-Based</span>
              {isRule && <span className="ml-auto text-xs bg-accent text-white px-1.5 py-0.5 rounded">Active</span>}
            </div>
            <p className="text-xs text-text-secondary leading-relaxed">
              Deterministic pricing and reorder rules. Works without any API credentials. Predictable and auditable.
            </p>
          </button>

          {/* AI */}
          <button
            onClick={() => !isAi && switchStrategy('AI')}
            disabled={busy}
            className={`p-4 rounded-xl border-2 text-left transition-all ${
              isAi ? 'border-accent bg-accent/5' : 'border-border hover:border-stone-300'
            }`}
          >
            <div className="flex items-center gap-2 mb-2">
              <Cpu size={16} className={isAi ? 'text-accent' : 'text-text-secondary'} />
              <span className="text-sm font-medium">AI-Powered</span>
              {isAi && <span className="ml-auto text-xs bg-accent text-white px-1.5 py-0.5 rounded">Active</span>}
            </div>
            <p className="text-xs text-text-secondary leading-relaxed">
              LLM-generated recommendations with contextual reasoning. Falls back to rule-based if AI is unavailable.
            </p>
          </button>
        </div>
      </Card>

      {/* AI Status */}
      <Card className="p-6">
        <h2 className="text-sm font-semibold mb-4">AI Provider Status</h2>
        <div className={`flex items-start gap-3 p-4 rounded-lg ${strategy.aiConfigured ? 'bg-emerald-50' : 'bg-amber-50'}`}>
          {strategy.aiConfigured
            ? <CheckCircle size={16} className="text-emerald-600 flex-shrink-0 mt-0.5" />
            : <AlertCircle size={16} className="text-amber-600 flex-shrink-0 mt-0.5" />
          }
          <div>
            <p className={`text-sm font-medium ${strategy.aiConfigured ? 'text-emerald-800' : 'text-amber-800'}`}>
              {strategy.aiConfigured ? `${strategy.aiProvider.toUpperCase()} Connected` : 'AI Not Configured'}
            </p>
            <p className={`text-xs mt-1 ${strategy.aiConfigured ? 'text-emerald-700' : 'text-amber-700'}`}>
              {strategy.message}
            </p>
          </div>
        </div>

        {!strategy.aiConfigured && (
          <div className="mt-4 p-4 bg-stone-50 rounded-lg">
            <p className="text-xs font-medium text-text-secondary mb-2">To enable AI mode:</p>
            <ol className="text-xs text-text-secondary space-y-1 list-decimal list-inside">
              <li>Set <code className="bg-stone-200 px-1 rounded">AI_API_KEY</code> environment variable with your Groq or Gemini API key</li>
              <li>Optionally set <code className="bg-stone-200 px-1 rounded">AI_PROVIDER</code> to <code className="bg-stone-200 px-1 rounded">groq</code> or <code className="bg-stone-200 px-1 rounded">gemini</code></li>
              <li>Restart the backend server</li>
              <li>Switch strategy to AI above</li>
            </ol>
          </div>
        )}
      </Card>

      {/* Info */}
      <Card className="p-6">
        <h2 className="text-sm font-semibold mb-3">How Recommendations Work</h2>
        <div className="space-y-3 text-xs text-text-secondary">
          <p><strong className="text-text-primary">Automatic triggers:</strong> When an order reduces stock below the reorder threshold (INVENTORY_LOW) or demand velocity exceeds 3× the category average (DEMAND_SPIKE), recommendations are generated asynchronously.</p>
          <p><strong className="text-text-primary">Manual triggers:</strong> Use the "Generate Recommendations" button on any product to request an immediate analysis.</p>
          <p><strong className="text-text-primary">Human approval:</strong> All recommendations require explicit Accept or Reject. Accepting a pricing recommendation updates the product price. Accepting a reorder recommendation adds stock.</p>
          <p><strong className="text-text-primary">Fallback:</strong> If AI is selected but unavailable, rule-based logic is used automatically. Each recommendation records its source (AI or RULE_BASED).</p>
        </div>
      </Card>

      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
}
