export const formatCurrency = (value: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

export const formatDate = (iso: string) =>
  new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));

export const formatPercent = (value: number) =>
  `${(value * 100).toFixed(0)}%`;

export const priceDiff = (current: number, recommended: number) => {
  const diff = ((recommended - current) / current) * 100;
  return diff >= 0 ? `+${diff.toFixed(1)}%` : `${diff.toFixed(1)}%`;
};

export const stockStatus = (stock: number, threshold: number): 'out' | 'low' | 'ok' => {
  if (stock === 0) return 'out';
  if (stock < threshold) return 'low';
  return 'ok';
};
