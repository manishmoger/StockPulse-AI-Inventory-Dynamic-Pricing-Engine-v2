# StockPulse — AI Inventory & Dynamic Pricing Engine

A full-stack commerce operations tool that monitors inventory signals, generates intelligent pricing and reorder recommendations using AI or deterministic rules, and presents them to a human operator for approval.

---

## Problem Statement

ShopStream sells electronics, apparel, and home goods. Two problems:

1. Employees manually decide when to reorder stock — too slow, too error-prone.
2. Prices are reviewed manually — they don't respond to low inventory or demand spikes.

StockPulse solves this by detecting inventory and demand signals automatically, generating recommendations via AI or rule-based logic, and requiring explicit human approval before any price or stock change is applied.

## Evaluator Quick Start

This repository is designed to be evaluated from the code and README alone. No API key is required to run the complete workflow because the `RULE_BASED` strategy is deterministic and the AI strategy automatically falls back when the provider is unavailable.

```cmd
cd backend
mvn test
mvn spring-boot:run
```

In a second terminal:

```cmd
cd frontend
npm install
npm run build
npm run dev
```

Open http://localhost:5173. The application seeds eight products on startup. The shortest verification path is:

1. Open **Inventory** and find `SKU-APP-001` (Organic Cotton T-Shirt).
2. Click **Sale** or place an order for 3 units. Stock moves from 8 to 5, below its threshold of 15.
3. Wait up to 5 seconds. `INVENTORY_LOW` creates pending pricing and reorder suggestions asynchronously.
4. Review the suggestions inline or in **Recommendations**. Each shows trigger, source, confidence, and reasoning.
5. Accept the pricing suggestion. Only then does `Product.currentPrice` change.
6. Accept or reject the reorder suggestion. Accepting it increases simulated stock; rejecting it leaves stock unchanged.

For an AI-free check, leave `AI_API_KEY` empty or select `RULE_BASED` in **Settings**. For the AI path, configure credentials only through environment variables; never commit them.

## Rubric Map

| Evaluation area | Implementation evidence |
|---|---|
| Domain and API | `entity/`, `enums/`, `controller/`, `dto/`, `repository/` |
| Commerce engine | `strategy/CommerceAdvisor.java`, rule-based and AI implementations |
| AI integration | `ai/LLMGateway.java`, `PromptBuilder.java`, `AiResponseValidator.java` |
| Agentic loop | `event/`, `ProductService`, `RecommendationEventListener` with after-commit async events |
| Human checkpoint | `RecommendationService.decidePricing()` and `decideReorder()` |
| UI floor | `InventoryPage` inline suggestions, `RecommendationsPage`, polling, accept/reject controls |
| Architecture | `ADR.md`, including tradeoffs and sprint-2 extension seams |
| Verification | `backend/src/test/` plus `npm run build` |

The system never publishes a price or creates an order automatically. It observes a signal, queues recommendations, and waits for an explicit human decision.

---

## Features

- **8 seeded products** across Electronics, Apparel, and Home categories
- **Simulate orders** — reduces stock, increments demand velocity
- **Automatic triggers** — INVENTORY_LOW and DEMAND_SPIKE events fire asynchronously
- **AI recommendations** — Zycus LiteLLM, Groq, or Gemini with structured JSON output and validation
- **Rule-based fallback** — deterministic pricing and reorder logic, no API key required
- **Runtime strategy switching** — switch between AI and RULE_BASED without restart
- **Human approval checkpoint** — Accept or Reject each recommendation individually
- **Activity audit log** — every significant event is persisted and displayed
- **Dashboard metrics** — live counts, charts, and priority alerts from real DB data
- **Full REST API** — products, suggestions, decisions, dashboard, settings

---

## Architecture

```
Browser (React + Vite)
    │
    │  HTTP / JSON
    ▼
Spring Boot 3 (port 8080)
    ├── ProductController      → CRUD, simulate order, manual suggest
    ├── RecommendationController → list, accept, reject suggestions
    ├── DashboardController    → metrics, activity feed
    ├── SettingsController     → strategy read/write
    │
    ├── ProductService         → business logic, event publishing
    ├── RecommendationService  → generate & decide suggestions
    ├── RecommendationEventListener (@Async, after commit) → handles domain events
    │
    ├── CommerceAdvisor (interface)
    │   ├── RuleBasedCommerceAdvisor  → deterministic rules
    │   └── AiCommerceAdvisor         → LLM + validation + fallback
    │
    ├── LLMGateway             → HTTP to Zycus LiteLLM / Groq / Gemini
    ├── PromptBuilder          → trigger-specific prompts
    ├── AiResponseValidator    → JSON parsing + safety bounds
    │
    └── H2 (file-backed)       → Products, Suggestions, ActivityLogs
```

---

## Technology Stack

| Layer     | Technology                                      |
|-----------|-------------------------------------------------|
| Backend   | Java 17, Spring Boot 3.2.5, Maven               |
| Persistence | Spring Data JPA, H2 (file-backed)             |
| Validation | Jakarta Bean Validation                        |
| Async     | Spring ApplicationEventPublisher + @Async       |
| AI        | Zycus LiteLLM, Groq, or Gemini via HTTP         |
| Frontend  | React, Vite, TypeScript, Tailwind CSS           |
| Charts    | Recharts                                        |
| HTTP      | Axios                                           |
| Routing   | React Router v7                                 |
| Icons     | Lucide React                                    |
| Tests     | JUnit 5, Spring Boot Test, MockMvc              |

---

## Folder Structure

```
stockpulse/
  backend/
    pom.xml
    src/main/java/com/stockpulse/
      StockPulseApplication.java
      ai/          LLMGateway, PromptBuilder, AiResponseValidator
      config/      AsyncConfig, CorsConfig, StrategyConfig
      controller/  ProductController, RecommendationController,
                   DashboardController, SettingsController
      dto/         Request and response DTOs
      entity/      Product, PricingSuggestion, ReorderSuggestion, ActivityLog
      enums/       ProductCategory, ProductStatus, SuggestionStatus, ...
      event/       InventoryLowEvent, DemandSpikeEvent
      exception/   GlobalExceptionHandler, ResourceNotFoundException
      repository/  JPA repositories
      seed/        DataSeeder (idempotent, runs on startup)
      service/     ProductService, RecommendationService,
                   RecommendationEventListener, DashboardService, ActivityService
      strategy/    CommerceAdvisor, RuleBasedCommerceAdvisor, AiCommerceAdvisor,
                   AdvisorContext, PricingRecommendation, ReorderRecommendation
    src/main/resources/
      application.properties
    src/test/java/com/stockpulse/
      RuleBasedCommerceAdvisorTest.java   (7 unit tests)
      StockPulseIntegrationTest.java      (9 integration tests)
  frontend/
    src/
      api/         client.ts (Axios), index.ts (all API functions)
      components/  ui.tsx (Badge, Button, Card, Toast, ConfidenceBar, ...)
      layouts/     AppLayout.tsx (sidebar + topbar)
      pages/       OverviewPage, InventoryPage, RecommendationsPage,
                   ActivityPage, SettingsPage
      types/       index.ts (TypeScript interfaces matching backend DTOs)
      utils/       index.ts (formatCurrency, formatDate, priceDiff, ...)
      styles/      index.css (Tailwind + Inter font)
  README.md
  ADR.md
  .gitignore
  .env.example
```

---

## Prerequisites

- Java 17 or later: `java -version`
- Maven 3.8+: `mvn -version`
- Node.js 18+: `node -v`
- npm 9+: `npm -v`

---

## Windows Setup & Run

### 1. Clone / extract the project

```cmd
cd C:\Users\Zycus\Desktop\Hackathon\stockpulse
```

### 2. Start the backend

```cmd
cd backend
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**.  
H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/stockpulse`)

### 3. Start the frontend (new terminal)

```cmd
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

---

## Environment Variable Configuration

Use `.env.example` as a reference, then set the variables in your shell or IDE run configuration. Spring Boot does not load a plain `.env` file automatically.

| Variable                  | Default          | Description                              |
|---------------------------|------------------|------------------------------------------|
| `AI_PROVIDER`             | `zycus`          | `zycus`, `groq`, or `gemini`             |
| `AI_API_KEY`              | *(empty)*        | Provider API key                         |
| `AI_MODEL`                | `qwen-cursor`    | Model name for the provider              |
| `AI_BASE_URL`             | provider default | Custom OpenAI-compatible endpoint        |
| `AI_PRODUCT_HEADER`       | `PC1`            | Required product header for Zycus         |
| `AI_COOKIE`               | *(empty)*        | Optional local session cookie; never commit it |
| `AI_TIMEOUT_SECONDS`      | `30`             | HTTP timeout for AI requests             |
| `DEMAND_SPIKE_MULTIPLIER` | `3.0`            | Multiplier for demand spike detection    |

### Setting environment variables on Windows (cmd)

```cmd
set AI_API_KEY=gsk_your_groq_key_here
set AI_PROVIDER=groq
cd backend
mvn spring-boot:run
```

Or create a `.env` file and use a tool like `dotenv-cli`, or set them in your IDE run configuration.

---

## AI Provider Setup

### Zycus LiteLLM (hackathon gateway)

Set the provider and model supplied with the problem statement. Keep the API key and optional cookie in environment variables only:

```cmd
set AI_PROVIDER=zycus
set AI_API_KEY=your_rotated_key
set AI_MODEL=qwen-cursor
set AI_BASE_URL=https://litellm-qc.zycus.net/v1/chat/completions
set AI_PRODUCT_HEADER=PC1
```

### Groq (alternative)

1. Sign up at https://console.groq.com
2. Create an API key
3. Set `AI_API_KEY=gsk_...` and `AI_PROVIDER=groq`
4. Default model: `llama3-8b-8192` (fast, free)

### Gemini

1. Get an API key from https://aistudio.google.com
2. Set `AI_API_KEY=AIza...` and `AI_PROVIDER=gemini`
3. Set `AI_MODEL=gemini-1.5-flash` or similar

### Rule-Based Mode (no credentials needed)

Leave `AI_API_KEY` empty. The application works fully in RULE_BASED mode.  
Switch strategy on the Settings page. If AI is selected but the key is missing, it falls back to rule-based automatically.

---

## Database Configuration

H2 file-backed database stored at `backend/data/stockpulse.mv.db`.  
The file persists between restarts. Seed data is idempotent (skips existing SKUs).

To reset the database: delete `backend/data/` and restart.

---

## API Endpoint Reference

### Products

| Method | Path                          | Description                    |
|--------|-------------------------------|--------------------------------|
| POST   | /products                     | Create product                 |
| GET    | /products                     | List (filter: status, category, search) |
| GET    | /products/{id}                | Get single product             |
| PATCH  | /products/{id}/stock          | Update stock level             |
| POST   | /products/{id}/orders         | Simulate sale                  |
| POST   | /products/{id}/suggest-pricing | Manual pricing suggestion     |
| POST   | /products/{id}/suggest-reorder | Manual reorder suggestion     |

### Suggestions

| Method | Path                          | Description                    |
|--------|-------------------------------|--------------------------------|
| GET    | /pricing-suggestions          | List (filter: status)          |
| PATCH  | /pricing-suggestions/{id}     | Accept or reject               |
| GET    | /reorder-suggestions          | List (filter: status)          |
| PATCH  | /reorder-suggestions/{id}     | Accept or reject               |

### Dashboard & Settings

| Method | Path                  | Description              |
|--------|-----------------------|--------------------------|
| GET    | /dashboard/metrics    | All dashboard metrics    |
| GET    | /activity             | Activity log (desc)      |
| GET    | /settings/strategy    | Current strategy         |
| PUT    | /settings/strategy    | Switch strategy          |

### Sample Requests

```bash
# Create product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"TST-001","name":"Test Item","category":"ELECTRONICS","currentPrice":49.99,"stockLevel":10,"reorderThreshold":20}'

# Simulate order after copying the ID for SKU-APP-001 from GET /products
curl -X POST http://localhost:8080/products/{productId}/orders \
  -H "Content-Type: application/json" \
  -d '{"quantity":3}'

# Accept pricing suggestion after copying its ID from GET /pricing-suggestions?status=PENDING
curl -X PATCH http://localhost:8080/pricing-suggestions/{suggestionId} \
  -H "Content-Type: application/json" \
  -d '{"decision":"ACCEPTED"}'

# Switch strategy without restarting
curl -X PUT http://localhost:8080/settings/strategy \
  -H "Content-Type: application/json" \
  -d '{"strategy":"AI"}'
```

---

## Test Commands

```cmd
cd backend

# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest=RuleBasedCommerceAdvisorTest

# Run only integration tests
mvn test -Dtest=StockPulseIntegrationTest
```

**Test results (last run):**
- `AiResponseValidatorTest`: 10 tests, 0 failures (broken LLM output: empty JSON, absurd price, zero price, negative qty, missing fields, direction inconsistency, confidence out of range, markdown fences, unparseable input)
- `RuleBasedCommerceAdvisorTest`: 7 tests, 0 failures
- `StockPulseIntegrationTest`: 9 tests, 0 failures
- **Total: 26 tests, 0 failures**

The frontend check is:

```cmd
cd frontend
npm run build
```

---

## Demo Workflow

1. Open http://localhost:5173
2. Dashboard shows 8 seeded products with low-stock and demand-spike alerts
3. Go to **Inventory** → find PRD-003 (Organic Cotton T-Shirt, stock=8, threshold=15)
4. Click the cart icon → simulate a sale of 3 units
5. Stock drops to 5 (below threshold=15) → INVENTORY_LOW event fires asynchronously
6. Wait 2–3 seconds. Pending pricing and reorder suggestions appear inline in the Inventory page and in **Recommendations**
7. Two PENDING suggestions appear: pricing (INCREASE) and reorder
8. Inspect reasoning and confidence
9. Click **Accept** on the pricing suggestion → product price updates
10. Click **Reject** on the reorder suggestion → stock unchanged
11. Go to **Activity** → see all events in reverse chronological order
12. Go to **Settings** → switch strategy to AI (requires a configured provider) or back to RULE_BASED
13. Repeat the demo — PRD-008 (Hoodie, demandVelocity=15) triggers DEMAND_SPIKE

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 8080 in use | `netstat -ano \| findstr :8080` then kill the PID |
| Port 5173 in use | `npm run dev -- --port 5174` |
| H2 lock error | Delete `backend/data/` and restart |
| AI returns 401 | Check `AI_API_KEY` is set correctly |
| AI returns 429 | Rate limit hit — switch to RULE_BASED temporarily |
| Recommendations not appearing | Wait 3–5 seconds; the Inventory and Recommendations pages poll asynchronously |
| CORS error in browser | Ensure backend is running on port 8080 |

---

## Known Limitations

- No authentication — this is a demo/hackathon application
- Demand velocity is a simple counter (not a rolling 24h window)
- AI model responses vary; validation bounds prevent unsafe recommendations
- File-backed H2 is not suitable for production (use PostgreSQL)
- No real supplier integration — reorder acceptance only increments stock in DB
