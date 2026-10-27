import { useEffect, useState } from 'react';
import { Search, Plus, RefreshCw, ShoppingCart, Edit2, X } from 'lucide-react';
import {
  getProducts, getPricingSuggestions, getReorderSuggestions, decidePricing, decideReorder,
  simulateOrder, updateStock, createProduct, suggestPricing, suggestReorder,
} from '../api';
import type {
  Product, ProductCategory, ProductStatus, CreateProductRequest,
  PricingSuggestion, ReorderSuggestion,
} from '../types';
import {
  Badge, Button, Card, Spinner, EmptyState, ProductStatusBadge, TriggerBadge, StockBar, Toast,
} from '../components/ui';
import { formatCurrency, formatDate } from '../utils';
import { usePolling } from '../hooks/usePolling';

const CATEGORIES: ProductCategory[] = ['ELECTRONICS', 'APPAREL', 'HOME'];

export default function InventoryPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [pricingSuggestions, setPricingSuggestions] = useState<PricingSuggestion[]>([]);
  const [reorderSuggestions, setReorderSuggestions] = useState<ReorderSuggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<ProductCategory | ''>('');
  const [status, setStatus] = useState<ProductStatus | ''>('');
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);
  const [orderModal, setOrderModal] = useState<Product | null>(null);
  const [stockModal, setStockModal] = useState<Product | null>(null);
  const [createModal, setCreateModal] = useState(false);
  const [detailProduct, setDetailProduct] = useState<Product | null>(null);
  const [qty, setQty] = useState(1);
  const [newStock, setNewStock] = useState(0);
  const [busy, setBusy] = useState(false);

  const load = (showLoading = true) => {
    if (showLoading) setLoading(true);
    Promise.all([
      getProducts({
        search: search || undefined,
        category: category || undefined,
        status: status || undefined,
      }),
      getPricingSuggestions('PENDING'),
      getReorderSuggestions('PENDING'),
    ])
      .then(([nextProducts, nextPricing, nextReorder]) => {
        setProducts(nextProducts);
        setPricingSuggestions(nextPricing);
        setReorderSuggestions(nextReorder);
      })
      .catch((e) => showToast(e.message, 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [search, category, status]);
  usePolling(() => load(false), 4000);

  const showToast = (msg: string, type: 'success' | 'error') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const handleQuickOrder = async (p: Product) => {
    setBusy(true);
    try {
      await simulateOrder(p.id, 1);
      showToast(`Sale of 1 unit placed for ${p.name} — check Recommendations in ~3s`, 'success');
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  const handleOrder = async () => {
    if (!orderModal) return;
    setBusy(true);
    try {
      await simulateOrder(orderModal.id, qty);
      showToast(`Order of ${qty} units placed for ${orderModal.name}`, 'success');
      setOrderModal(null);
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  const handleStockUpdate = async () => {
    if (!stockModal) return;
    setBusy(true);
    try {
      await updateStock(stockModal.id, newStock);
      showToast('Stock updated', 'success');
      setStockModal(null);
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  const handleSuggest = async (p: Product) => {
    setBusy(true);
    try {
      await Promise.all([suggestPricing(p.id), suggestReorder(p.id)]);
      showToast('Recommendations generated', 'success');
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  const handlePricingDecision = async (id: number, decision: 'ACCEPTED' | 'REJECTED') => {
    setBusy(true);
    try {
      await decidePricing(id, decision);
      showToast(decision === 'ACCEPTED' ? 'Price updated successfully' : 'Pricing recommendation rejected', 'success');
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  const handleReorderDecision = async (id: number, decision: 'ACCEPTED' | 'REJECTED') => {
    setBusy(true);
    try {
      await decideReorder(id, decision);
      showToast(decision === 'ACCEPTED' ? 'Stock replenishment applied' : 'Reorder recommendation rejected', 'success');
      load();
    } catch (e: any) {
      showToast(e.message, 'error');
    } finally { setBusy(false); }
  };

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-48">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name or SKU…"
            className="w-full pl-9 pr-3 py-2 text-sm border border-border rounded-lg bg-surface focus:outline-none focus:ring-2 focus:ring-accent/30"
          />
        </div>
        <select value={category} onChange={(e) => setCategory(e.target.value as any)}
          className="px-3 py-2 text-sm border border-border rounded-lg bg-surface focus:outline-none">
          <option value="">All Categories</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value as any)}
          className="px-3 py-2 text-sm border border-border rounded-lg bg-surface focus:outline-none">
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="OUT_OF_STOCK">Out of Stock</option>
          <option value="PRICE_REVIEW_PENDING">Review Pending</option>
        </select>
        <Button onClick={load} variant="ghost" size="sm"><RefreshCw size={13} /></Button>
        <Button onClick={() => setCreateModal(true)} variant="primary" size="sm">
          <Plus size={13} /> New Product
        </Button>
      </div>

      {/* Table */}
      <Card>
        {loading ? (
          <div className="flex justify-center py-16"><Spinner size="lg" /></div>
        ) : products.length === 0 ? (
          <EmptyState icon={<Package size={32} />} title="No products found" description="Try adjusting your filters" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {['Product', 'SKU', 'Category', 'Price', 'Stock', 'Demand/24h', 'Status', 'Actions'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-medium text-text-secondary uppercase tracking-wide">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {products.map((p) => (
                  <tr key={p.id} className="hover:bg-stone-50 transition-colors">
                    <td className="px-4 py-3">
                      <button onClick={() => setDetailProduct(p)} className="font-medium text-text-primary hover:text-accent text-left">
                        {p.name}
                      </button>
                      <InlineRecommendations
                        pricing={pricingSuggestions.filter((s) => s.productId === p.id)}
                        reorder={reorderSuggestions.filter((s) => s.productId === p.id)}
                        busy={busy}
                        onPricingDecision={handlePricingDecision}
                        onReorderDecision={handleReorderDecision}
                      />
                    </td>
                    <td className="px-4 py-3 text-text-secondary font-mono text-xs">{p.sku}</td>
                    <td className="px-4 py-3">
                      <Badge>{p.category}</Badge>
                    </td>
                    <td className="px-4 py-3 font-medium">{formatCurrency(p.currentPrice)}</td>
                    <td className="px-4 py-3 w-32">
                      <StockBar stock={p.stockLevel} threshold={p.reorderThreshold} />
                      <span className="text-xs text-text-muted">threshold: {p.reorderThreshold}</span>
                    </td>
                    <td className="px-4 py-3 text-text-secondary">{p.demandVelocity}</td>
                    <td className="px-4 py-3"><ProductStatusBadge status={p.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <Button size="sm" variant="primary" onClick={() => handleQuickOrder(p)}
                          disabled={p.stockLevel === 0 || busy}>
                          <ShoppingCart size={12} /> Sale
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => { setOrderModal(p); setQty(1); }}
                          disabled={p.stockLevel === 0}>
                          <Edit2 size={12} />
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => { setStockModal(p); setNewStock(p.stockLevel); }}>
                          <Edit2 size={12} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Order Modal */}
      {orderModal && (
        <Modal title={`Simulate Sale — ${orderModal.name}`} onClose={() => setOrderModal(null)}>
          <p className="text-sm text-text-secondary mb-4">
            Available stock: <strong>{orderModal.stockLevel}</strong> units
          </p>
          <label className="block text-xs font-medium text-text-secondary mb-1">Quantity</label>
          <input type="number" min={1} max={orderModal.stockLevel} value={qty}
            onChange={(e) => setQty(Number(e.target.value))}
            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent/30" />
          <div className="flex justify-end gap-2 mt-4">
            <Button onClick={() => setOrderModal(null)}>Cancel</Button>
            <Button variant="primary" onClick={handleOrder} disabled={busy || qty < 1 || qty > orderModal.stockLevel}>
              {busy ? <Spinner size="sm" /> : 'Place Order'}
            </Button>
          </div>
        </Modal>
      )}

      {/* Stock Modal */}
      {stockModal && (
        <Modal title={`Update Stock — ${stockModal.name}`} onClose={() => setStockModal(null)}>
          <label className="block text-xs font-medium text-text-secondary mb-1">New Stock Level</label>
          <input type="number" min={0} value={newStock}
            onChange={(e) => setNewStock(Number(e.target.value))}
            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent/30" />
          <div className="flex justify-end gap-2 mt-4">
            <Button onClick={() => setStockModal(null)}>Cancel</Button>
            <Button variant="primary" onClick={handleStockUpdate} disabled={busy || newStock < 0}>
              {busy ? <Spinner size="sm" /> : 'Update Stock'}
            </Button>
          </div>
        </Modal>
      )}

      {/* Create Modal */}
      {createModal && <CreateProductModal onClose={() => setCreateModal(false)} onCreated={() => { setCreateModal(false); load(); showToast('Product created', 'success'); }} />}

      {/* Detail Drawer */}
      {detailProduct && (
        <Modal title={detailProduct.name} onClose={() => setDetailProduct(null)}>
          <div className="space-y-3 text-sm">
            <Row label="SKU" value={detailProduct.sku} />
            <Row label="Category" value={detailProduct.category} />
            <Row label="Price" value={formatCurrency(detailProduct.currentPrice)} />
            <Row label="Stock" value={`${detailProduct.stockLevel} units`} />
            <Row label="Reorder Threshold" value={`${detailProduct.reorderThreshold} units`} />
            <Row label="Demand Velocity" value={`${detailProduct.demandVelocity} orders/24h`} />
            <Row label="Status" value={<ProductStatusBadge status={detailProduct.status} />} />
            <Row label="Created" value={formatDate(detailProduct.createdAt)} />
          </div>
          <div className="flex gap-2 mt-5">
            <Button variant="primary" size="sm" onClick={() => { handleSuggest(detailProduct); setDetailProduct(null); }} disabled={busy}>
              Generate Recommendations
            </Button>
            <Button size="sm" onClick={() => setDetailProduct(null)}>Close</Button>
          </div>
        </Modal>
      )}

      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
}

function InlineRecommendations({
  pricing, reorder, busy, onPricingDecision, onReorderDecision,
}: {
  pricing: PricingSuggestion[];
  reorder: ReorderSuggestion[];
  busy: boolean;
  onPricingDecision: (id: number, decision: 'ACCEPTED' | 'REJECTED') => void;
  onReorderDecision: (id: number, decision: 'ACCEPTED' | 'REJECTED') => void;
}) {
  if (pricing.length === 0 && reorder.length === 0) return null;

  return (
    <div className="mt-2 space-y-1.5 max-w-xl">
      {pricing.map((suggestion) => (
        <div key={`pricing-${suggestion.id}`} className="flex flex-wrap items-center gap-1.5 text-xs">
          <Badge variant="warning">Pricing</Badge>
          <TriggerBadge trigger={suggestion.triggerReason} />
          <span className="text-text-secondary">
            {formatCurrency(suggestion.currentPrice)} → {formatCurrency(suggestion.recommendedPrice)} ({suggestion.direction}) · {Math.round(suggestion.confidence * 100)}%
          </span>
          <button className="text-emerald-700 hover:underline disabled:opacity-50" disabled={busy}
            onClick={() => onPricingDecision(suggestion.id, 'ACCEPTED')}>Accept</button>
          <button className="text-red-700 hover:underline disabled:opacity-50" disabled={busy}
            onClick={() => onPricingDecision(suggestion.id, 'REJECTED')}>Reject</button>
        </div>
      ))}
      {reorder.map((suggestion) => (
        <div key={`reorder-${suggestion.id}`} className="flex flex-wrap items-center gap-1.5 text-xs">
          <Badge variant="info">Reorder</Badge>
          <TriggerBadge trigger={suggestion.triggerReason} />
          <span className="text-text-secondary">
            +{suggestion.recommendedQuantity} units · {Math.round(suggestion.confidence * 100)}%
          </span>
          <button className="text-emerald-700 hover:underline disabled:opacity-50" disabled={busy}
            onClick={() => onReorderDecision(suggestion.id, 'ACCEPTED')}>Accept</button>
          <button className="text-red-700 hover:underline disabled:opacity-50" disabled={busy}
            onClick={() => onReorderDecision(suggestion.id, 'REJECTED')}>Reject</button>
        </div>
      ))}
    </div>
  );
}

function Package({ size }: { size: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M16.5 9.4l-9-5.19M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>;
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between items-center py-1.5 border-b border-border last:border-0">
      <span className="text-text-secondary">{label}</span>
      <span className="font-medium text-text-primary">{value}</span>
    </div>
  );
}

function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 backdrop-blur-sm">
      <div className="bg-surface rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold">{title}</h2>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary"><X size={16} /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

function CreateProductModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [form, setForm] = useState<CreateProductRequest>({
    sku: '', name: '', category: 'ELECTRONICS', currentPrice: 0, stockLevel: 0, reorderThreshold: 10, demandVelocity: 0,
  });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const set = (k: keyof CreateProductRequest, v: any) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async () => {
    setBusy(true);
    setErr('');
    try {
      await createProduct(form);
      onCreated();
    } catch (e: any) {
      setErr(e.message);
    } finally { setBusy(false); }
  };

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/30 backdrop-blur-sm">
      <div className="bg-surface rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold">Create Product</h2>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary"><X size={16} /></button>
        </div>
        <div className="space-y-3">
          {[
            { label: 'SKU', key: 'sku', type: 'text' },
            { label: 'Name', key: 'name', type: 'text' },
            { label: 'Price ($)', key: 'currentPrice', type: 'number' },
            { label: 'Stock Level', key: 'stockLevel', type: 'number' },
            { label: 'Reorder Threshold', key: 'reorderThreshold', type: 'number' },
            { label: 'Demand Velocity', key: 'demandVelocity', type: 'number' },
          ].map(({ label, key, type }) => (
            <div key={key}>
              <label className="block text-xs font-medium text-text-secondary mb-1">{label}</label>
              <input type={type} value={(form as any)[key]}
                onChange={(e) => set(key as any, type === 'number' ? Number(e.target.value) : e.target.value)}
                className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-accent/30" />
            </div>
          ))}
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">Category</label>
            <select value={form.category} onChange={(e) => set('category', e.target.value as ProductCategory)}
              className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none">
              {['ELECTRONICS', 'APPAREL', 'HOME'].map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          {err && <p className="text-xs text-red-600">{err}</p>}
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={submit} disabled={busy}>
            {busy ? <Spinner size="sm" /> : 'Create'}
          </Button>
        </div>
      </div>
    </div>
  );
}
