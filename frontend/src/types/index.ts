export type ProductCategory = 'ELECTRONICS' | 'APPAREL' | 'HOME';
export type ProductStatus = 'ACTIVE' | 'PRICE_REVIEW_PENDING' | 'OUT_OF_STOCK';
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';
export type TriggerReason = 'INITIAL' | 'INVENTORY_LOW' | 'DEMAND_SPIKE' | 'MANUAL';
export type PriceDirection = 'INCREASE' | 'DECREASE' | 'HOLD';
export type RecommendationSource = 'AI' | 'RULE_BASED';
export type ActivityType =
  | 'PRODUCT_CREATED'
  | 'STOCK_UPDATED'
  | 'ORDER_SIMULATED'
  | 'PRICING_RECOMMENDATION_GENERATED'
  | 'REORDER_RECOMMENDATION_GENERATED'
  | 'RECOMMENDATION_ACCEPTED'
  | 'RECOMMENDATION_REJECTED'
  | 'STRATEGY_CHANGED';

export interface Product {
  id: number;
  sku: string;
  name: string;
  category: ProductCategory;
  currentPrice: number;
  costPrice?: number;
  stockLevel: number;
  reorderThreshold: number;
  demandVelocity: number;
  status: ProductStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PricingSuggestion {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  currentPrice: number;
  recommendedPrice: number;
  direction: PriceDirection;
  confidence: number;
  reasoning: string;
  status: SuggestionStatus;
  triggerReason: TriggerReason;
  source: RecommendationSource;
  createdAt: string;
  decidedAt?: string;
}

export interface ReorderSuggestion {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  currentStock: number;
  recommendedQuantity: number;
  suggestedLeadTimeDays: number;
  confidence: number;
  reasoning: string;
  status: SuggestionStatus;
  triggerReason: TriggerReason;
  source: RecommendationSource;
  createdAt: string;
  decidedAt?: string;
}

export interface ActivityLog {
  id: number;
  type: ActivityType;
  productSku: string;
  productName: string;
  details: string;
  createdAt: string;
}

export interface DashboardMetrics {
  totalProducts: number;
  lowStockCount: number;
  outOfStockCount: number;
  pendingPricingSuggestions: number;
  pendingReorderSuggestions: number;
  demandSpikeCount: number;
  inventoryStatusDistribution: Record<string, number>;
  recentActivity: ActivityLog[];
}

export interface StrategyResponse {
  activeStrategy: string;
  aiConfigured: boolean;
  aiProvider: string;
  message: string;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  category: ProductCategory;
  currentPrice: number;
  costPrice?: number;
  stockLevel: number;
  reorderThreshold: number;
  demandVelocity?: number;
}
