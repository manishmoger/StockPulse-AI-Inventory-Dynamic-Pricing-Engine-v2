import api from './client';
import type {
  Product, PricingSuggestion, ReorderSuggestion,
  ActivityLog, DashboardMetrics, StrategyResponse,
  CreateProductRequest, ProductStatus, ProductCategory, SuggestionStatus,
} from '../types';

// Products
export const getProducts = (params?: { status?: ProductStatus; category?: ProductCategory; search?: string }) =>
  api.get<Product[]>('/products', { params }).then((r) => r.data);

export const getProduct = (id: number) =>
  api.get<Product>(`/products/${id}`).then((r) => r.data);

export const createProduct = (data: CreateProductRequest) =>
  api.post<Product>('/products', data).then((r) => r.data);

export const updateStock = (id: number, stockLevel: number) =>
  api.patch<Product>(`/products/${id}/stock`, { stockLevel }).then((r) => r.data);

export const simulateOrder = (id: number, quantity: number) =>
  api.post<Product>(`/products/${id}/orders`, { quantity }).then((r) => r.data);

export const suggestPricing = (id: number) =>
  api.post<PricingSuggestion>(`/products/${id}/suggest-pricing`).then((r) => r.data);

export const suggestReorder = (id: number) =>
  api.post<ReorderSuggestion>(`/products/${id}/suggest-reorder`).then((r) => r.data);

// Pricing suggestions
export const getPricingSuggestions = (status?: SuggestionStatus) =>
  api.get<PricingSuggestion[]>('/pricing-suggestions', { params: status ? { status } : {} }).then((r) => r.data);

export const decidePricing = (id: number, decision: SuggestionStatus) =>
  api.patch<PricingSuggestion>(`/pricing-suggestions/${id}`, { decision }).then((r) => r.data);

// Reorder suggestions
export const getReorderSuggestions = (status?: SuggestionStatus) =>
  api.get<ReorderSuggestion[]>('/reorder-suggestions', { params: status ? { status } : {} }).then((r) => r.data);

export const decideReorder = (id: number, decision: SuggestionStatus) =>
  api.patch<ReorderSuggestion>(`/reorder-suggestions/${id}`, { decision }).then((r) => r.data);

// Dashboard
export const getDashboardMetrics = () =>
  api.get<DashboardMetrics>('/dashboard/metrics').then((r) => r.data);

// Activity
export const getActivity = () =>
  api.get<ActivityLog[]>('/activity').then((r) => r.data);

// Settings
export const getStrategy = () =>
  api.get<StrategyResponse>('/settings/strategy').then((r) => r.data);

export const setStrategy = (strategy: string) =>
  api.put<StrategyResponse>('/settings/strategy', { strategy }).then((r) => r.data);
