# Architecture Decision Records — StockPulse

> Format: Context → Options → Decision → Tradeoffs
> Each decision points to the actual code that implements it.

## Submission Reading Guide

Read ADR-1 for the service boundary, ADR-2 for the advisor contract, ADR-3 for runtime switching, ADR-4 for AI resilience, ADR-5 for event-driven behavior and idempotency, and ADR-6 for extensibility and deliberate exclusions. Each decision names the implementation package or class so the design can be verified directly from the repository.

### Evaluation checklist

- **Observe:** stock or demand changes publish an inventory signal.
- **Reason:** the active `CommerceAdvisor` produces pricing and reorder recommendations.
- **Act:** the after-commit async listener persists both suggestions as `PENDING`.
- **Checkpoint:** a human accepts or rejects each suggestion; prices never change automatically.
- **Fallback:** an unavailable or invalid AI response produces a visible rule-based suggestion.

---

## Why this is "agentic" rather than merely automated

A scheduled job that runs every 5 minutes and checks stock levels is **automated** — it acts on a clock, not on a signal.

StockPulse is **agentic** because:

1. **Trigger is a signal change, not a cron.** `ProductService.publishEventsIfNeeded()` fires only when a real domain event occurs — an order is placed or stock is patched. No polling. See `event/InventoryLowEvent.java` and `event/DemandSpikeEvent.java`.

2. **Action is queuing, not publishing.** The after-commit async handler creates a `PricingSuggestion` and `ReorderSuggestion` with `status = PENDING`. Nothing is applied to the product. See `service/RecommendationEventListener.java`.

3. **Checkpoint is human approval.** `PATCH /pricing-suggestions/{id}` with `{"decision":"ACCEPTED"}` is the only path that calls `product.setCurrentPrice(...)`. The system cannot change a price on its own. See `service/RecommendationService.decidePricing()`.

---

## ADR-1: Where does commerce logic live?

**Context**  
Pricing and reorder logic could live in controllers (bad — mixes HTTP concerns), in a single `ProductService` (god-class risk), or in a dedicated layer.

**Options**  
A. Embed in `ProductService` — one class, simple to find.  
B. Dedicated `CommerceAdvisor` interface + implementations, called by a thin `RecommendationService`.

**Decision**  
Option B. `CommerceAdvisor` (`strategy/CommerceAdvisor.java`) is the boundary. `RecommendationService` calls it; controllers call `RecommendationService`. `ProductService` only manages product state and publishes events — it has no pricing logic.

**Tradeoffs**  
- `ProductService` stays focused on stock/status state machines.
- `RecommendationService` stays focused on suggestion lifecycle.
- Adding a new advisor (e.g. `CompetitorAwareCommerceAdvisor`) requires zero changes to controllers or services — only a new `@Component("COMPETITOR")` implementing the interface.
- Slight indirection: two service classes instead of one.

---

## ADR-2: Unified AI call vs separate pricing/reorder calls

**Context**  
The AI could be called once (returning both pricing and reorder in one JSON response) or twice (one call per recommendation type).

**Options**  
A. Two calls — independent fallbacks, cleaner contracts, but double latency and double token cost.  
B. One call — single prompt returns `{"pricing":{...},"reorder":{...}}`, one round-trip.

**Decision**  
Option A at the advisor boundary. `CommerceAdvisor` exposes separate pricing and reorder methods, and `AiCommerceAdvisor` uses the same structured prompt/response contract for each method. This keeps the HTTP endpoints and async listener independent: a pricing failure can fall back without preventing a reorder suggestion, and vice versa. The current implementation validates the combined JSON shape returned by each gateway request through `AiResponseValidator.parse()` and selects the requested section.

**Tradeoffs**  
- Two gateway requests can cost more and add latency compared with one combined call.
- Separate contracts make fallback behavior and testing clearer for each suggestion type.
- A future combined-call implementation can be added behind `CommerceAdvisor` without changing controllers or event handlers.

---

## ADR-3: Runtime strategy switching

**Context**  
The active strategy (RULE_BASED vs AI) must be switchable at runtime — from both the HTTP path (`PUT /settings/strategy`) and the async event path — without a restart and without an `if/else` in every caller.

**Options**  
A. `if/else` or `switch` in `RecommendationService` — simple but requires code change for every new strategy.  
B. Spring bean registry: each strategy is a `@Component` named by its key; a config holder resolves the active bean by name.

**Decision**  
Option B. `StrategyConfig` holds the active name in an `AtomicReference<String>` (thread-safe for concurrent requests). `getActiveAdvisor()` calls `applicationContext.getBean(name, CommerceAdvisor.class)`. Both `RecommendationService` (HTTP path) and `RecommendationEventListener` (async path) call the same `strategyConfig.getActiveAdvisor()`.

**Sprint-2 seam:** A `CompetitorAwareCommerceAdvisor` needs only `@Component("COMPETITOR")` and one `PUT /settings/strategy` call with `{"strategy":"COMPETITOR"}`. Zero changes to existing code.

**Tradeoffs**  
- `AtomicReference` resets to `"AI"` on restart (acceptable for demo; persist to DB for production).
- Bean lookup by string name is slightly less type-safe than a typed registry — mitigated by the validation in `StrategyConfig.setActiveStrategy()`.

---

## ADR-4: LLM failure handling

**Context**  
The AI call can fail in many ways: network timeout, HTTP 429/500, empty body, non-JSON response, JSON missing required fields, or numerically absurd values (price = $0, price = $999,999, quantity = -1).

**Options**  
A. Propagate the exception — suggestion is never created, silently lost.  
B. Catch all failure modes in `AiCommerceAdvisor`, fall back to `RuleBasedCommerceAdvisor`, annotate the reasoning with "AI fallback used".

**Decision**  
Option B. `AiCommerceAdvisor.recommendPricing()` and `recommendReorder()` each have a `try/catch(Exception)` that delegates to `fallback` (the injected `RuleBasedCommerceAdvisor`). The fallback result's `reasoning` field is prefixed with `[AI unavailable — rule-based fallback applied]` so the merchandiser always knows which engine was used. `AiResponseValidator` enforces:
- `recommendedPrice > 0` and within `[0.5×, 1.5×]` current price.
- `direction` consistent with the price delta (auto-corrected to HOLD if inconsistent).
- `confidence` in `[0.0, 1.0]` — throws if outside range.
- `recommendedQuantity` is a positive integer.
- `suggestedLeadTimeDays` is between 1 and 90.
- Both `reasoning` fields are non-empty.
- Markdown fences (` ```json `) are stripped before JSON parsing.

The saved suggestion's `source` field records `AI` or `RULE_BASED` so the UI badge shows which engine was used.

**Test coverage**  
`AiResponseValidatorTest` (10 tests) covers: valid response, markdown fence stripping, absurd price (too high), zero price, negative reorder quantity, empty JSON, missing pricing field, direction inconsistency auto-correction, confidence out of range, and completely unparseable input.

**Tradeoffs**  
- A suggestion is always created — never silently dropped.
- The fallback reasoning string notes "AI unavailable — rule-based fallback applied" so the merchandiser knows.
- Bounds are conservative (`1.5×` max) to prevent absurd AI outputs from reaching production prices.

---

## ADR-5: Agentic loop — triggers, async, idempotency, accept side effects

**Context**  
Stock and velocity changes must trigger suggestion generation without blocking the HTTP response. The same product can trigger both INVENTORY_LOW and DEMAND_SPIKE simultaneously. Repeated orders must not create duplicate PENDING suggestions.

**Trigger design**  
`ProductService.publishEventsIfNeeded()` is called after every `save()`. It checks two conditions independently and publishes `InventoryLowEvent` and/or `DemandSpikeEvent`. Both can fire for the same product on the same order.

**Async design**  
`RecommendationEventListener` handles both events with `@Async("taskExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)`. The thread pool (`AsyncConfig`) has 4 core / 8 max threads. The HTTP endpoint returns before the AI call starts, and the handler only reads committed product state.

**Idempotency**  
Before creating a suggestion, `RecommendationService` obtains a pessimistic write lock on the product row through `ProductRepository.findByIdForUpdate()`. The idempotency check (`existsPendingForProductAndTrigger`) and insert then run inside the same `@Transactional` boundary. The lock serializes concurrent async handlers for the same product, preventing duplicate PENDING suggestions. If a duplicate is detected, the existing PENDING suggestion is returned instead of creating a new one.

**Shared vs separate handlers**  
One listener class, two transactional event-listener methods. They share the same idempotency check and fallback logic. If both events fire for the same product, both handlers run concurrently on separate threads — the product-row lock prevents duplicate suggestions.

**Accept side effects (documented decisions)**  
- Accepting a `PricingSuggestion`: `product.currentPrice` is updated atomically in the same transaction. Product status transitions `PRICE_REVIEW_PENDING → ACTIVE` (unless `OUT_OF_STOCK`, which takes priority).
- Accepting a `ReorderSuggestion`: `product.stockLevel += recommendedQuantity` atomically. If the product was `OUT_OF_STOCK`, it transitions to `ACTIVE`.
- If one suggestion is accepted and the other is still PENDING: the PENDING suggestion remains valid and can still be accepted or rejected independently. Accepting the reorder does not auto-accept the pricing suggestion.
- Prices never change without an explicit human `ACCEPTED` decision.

**Tradeoffs**  
- In-process Spring events: no external broker needed, but events are lost if the JVM crashes after the transaction commits and before the async handler completes (acceptable for demo; use a transactional outbox for production).
- Pessimistic locking serializes suggestion generation per product and is simple for the demo. A durable idempotency key or database constraint would be preferable if the system moves to multiple application instances.

---

## ADR-6: Extensibility and deliberate exclusions

### Sprint-2 seams (pointing to actual code)

| Feature | Code seam |
|---|---|
| Margin floor guard | `Product.marginFloor` (nullable, `entity/Product.java`). `AiResponseValidator` can check `recommendedPrice >= product.marginFloor` before accepting AI output. `RuleBasedCommerceAdvisor` can enforce it in the pricing formula. |
| Cost-based pricing | `Product.costPrice` already in `AdvisorContext`. Pass to AI prompt and rule engine. |
| CompetitorAwareStrategy | Add `@Component("COMPETITOR")` implementing `CommerceAdvisor`. Register via `PUT /settings/strategy {"strategy":"COMPETITOR"}`. Zero changes to `RecommendationService` or event listeners. |
| Supplier catalog | `Product.supplierId` (nullable, `entity/Product.java`). A future `SupplierService` can look up lead times and MOQ when generating reorder suggestions. |
| Category pricing rules | `AdvisorContext.category` is already passed to both advisors. A `CategoryRuleRegistry` bean can be injected into `RuleBasedCommerceAdvisor` without changing the interface. |
| Price-change cooldown | Add `lastPriceChangedAt` to `Product`. Check in `RecommendationService.generatePricingSuggestion()` before creating a new suggestion. |
| Inventory snapshots | `InventorySnapshot` entity is written on every stock change and order. Sprint-2 can expose `GET /products/{id}/snapshots` for a price/stock history chart. |

### Sprint-3 items
- Auto-apply within guardrails (confidence > 0.95 + within marginFloor)
- Auto purchase order generation via supplier API
- Storefront price API (push accepted price to CDN/cache)
- Competitor price feed as an AI tool call
- Bundle pricing across SKUs

### Deliberate exclusions (priority decisions, not time excuses)

| Excluded | Reason |
|---|---|
| Storefront / cart / payments | Out of scope — this is an operations tool, not a customer-facing store |
| Authentication | Adds complexity with no demo value; add Spring Security + JWT in Sprint 2 |
| Competitor scraping | Requires external API contracts not available in hackathon; seam exists via `CompetitorAwareStrategy` |
| Auto purchase orders | Requires supplier API; `supplierId` seam is in place |
| Kafka / Redis / external brokers | Spring in-process events are sufficient for single-instance demo |
| SSE streaming | Deprioritized in favour of completing the agentic loop and ADR |

---

## Demo walkthrough script

**Path 1 — INVENTORY_LOW (PRD-003 / SKU-APP-001)**

```
1. GET /products?category=APPAREL
   → SKU-APP-001 "Organic Cotton T-Shirt", stock=8, threshold=15, status=PRICE_REVIEW_PENDING

2. POST /products/{id}/orders  {"quantity": 3}
   → stock drops to 5 (< threshold 15)
   → InventoryLowEvent published AFTER save commits
   → HTTP 200 returns immediately with updated product

3. (2–3 seconds later) GET /pricing-suggestions?status=PENDING
   → New PricingSuggestion appears: triggerReason=INVENTORY_LOW, source=AI (or RULE_BASED if AI down)
   → reasoning explains the stock-vs-threshold tradeoff

4. GET /reorder-suggestions?status=PENDING
   → New ReorderSuggestion: recommendedQuantity = max(1, 15×3 − 5) = 40

5. PATCH /pricing-suggestions/{id}  {"decision":"ACCEPTED"}
   → product.currentPrice updated atomically
   → product.status → ACTIVE
   → decidedAt recorded

6. PATCH /reorder-suggestions/{id}  {"decision":"REJECTED"}
   → stock unchanged, suggestion marked REJECTED
```

**Path 2 — DEMAND_SPIKE (PRD-008 / SKU-APP-003)**

```
1. POST /products/{id}/orders {"quantity": 5}  (repeat 3×)
   → demandVelocity climbs above 3× APPAREL category average
   → DemandSpikeEvent published

2. GET /pricing-suggestions?status=PENDING
   → triggerReason=DEMAND_SPIKE, AI reasoning discusses velocity vs peers

3. Accept or reject as above.
```

**Path 3 — AI down / fallback**

```
1. Set AI_API_KEY to an invalid value (or stop the network)
2. Trigger any order on a low-stock product
3. GET /pricing-suggestions?status=PENDING
   → suggestion appears with source=RULE_BASED
   → reasoning contains "AI unavailable — rule-based fallback applied"
   → application never drops the suggestion silently
```
