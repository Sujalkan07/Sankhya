# SANKHYA — GOOGLE GEMINI AI INTEGRATION ARCHITECTURE

**Project:** SANKHYA — AI-Powered Supermarket Analytics and Business Intelligence Platform  
**Document:** Google Gemini AI Integration Architecture & Grounded Reasoning  
**Phase:** Phase 5  
**Version:** 1.0.0  
**Model:** `gemini-2.5-flash` (with automated fallback to `gemini-1.5-flash`)  

---

## 1. Executive Summary & Design Principles

Sankhya incorporates Google's latest Gemini models to bridge quantitative telemetry with executive decision-making. The Gemini engine does not guess numbers; it acts as an **expert retail consultant grounded strictly in verified backend metrics and machine learning models**.

Key Architectural Guarantees:
1. **Zero Hallucination Grounding:** Prompts inject real, computed transactional metrics (revenue, margins, volume), K-Means cluster attributes, Apriori association rules, and Linear Regression forecasts. The system instruction strictly prohibits inventing imaginary figures.
2. **Zero PII Exposure:** Customer names, phone numbers, email addresses, and cashier IDs are scrubbed before prompt assembly. Only anonymized IDs, aggregated cohort sizes, and transactional totals are transmitted.
3. **Secret Protection:** The Gemini API key is loaded from the environment or gitignored `.env` file (`${GEMINI_API_KEY:}`). It is never committed, never written to client-side templates, and never echoed in server logs.
4. **Graceful Degradation:** If the API key is unconfigured or if Google returns rate-limit/quota errors (HTTP 429), the application gracefully serves verified deterministic analytics without throwing 500 errors.

---

## 2. Component Architecture

```mermaid
flowchart TD
    subgraph Spring Boot Backend
        ENV[.env / Environment Variable] -->|GEMINI_API_KEY| GC[GeminiConfig]
        GC --> CLIENT[GeminiClient]
        
        DB[(Database: Sales, Clusters, Rules)] --> PS[GeminiPromptService]
        PS -->|Scrubbed JSON Telemetry + Strict System Prompt| CLIENT
        
        CLIENT -->|HTTP POST v1beta/models/gemini-2.5-flash:generateContent| GOOG[Google Gemini API]
        GOOG -->|JSON Response Candidates + Token Counts| CLIENT
        
        CLIENT --> GIS[GeminiInsightService]
        CLIENT --> GCS[GeminiChatService]
        
        GIS -->|Persist Insights| ENT[(ai_gemini_insights Table)]
    end

    subgraph User Experience (UI)
        ENT --> VIEW[AI Insights & Dashboards]
        GCS <-->|AJAX JSON Chat| COPILOT[Interactive BI Copilot]
    end
```

---

## 3. Secret Management & Configuration

### 3.1 Loading Flow
1. `SankhyaApplication.java`:
   On startup, `loadDotEnv()` reads `.env` if present in the working directory and loads keys into `System.setProperty()` if not already set in OS environment variables.
2. `application.yml`:
   ```yaml
   sankhya:
     ai:
       gemini:
         api-key: ${GEMINI_API_KEY:}
         model: ${GEMINI_MODEL:gemini-2.5-flash}
         timeout-seconds: ${GEMINI_TIMEOUT:30}
   ```
3. `.gitignore`:
   Contains `.env`, `.env.local`, and secrets. Verified to prevent accidental commits.
4. `.env.example`:
   Committed template for developer onboarding.

---

## 4. Grounded Prompt Engineering

### 4.1 System Instruction
```text
You are Sankhya AI, an executive supermarket analytics and retail intelligence consultant. 
You provide data-grounded strategic insights, merchandise optimizations, and actionable advice to supermarket owners and category managers.
GUIDELINES:
1. STRICT GROUNDING: Rely strictly on the provided verified metrics, ML clusters, forecasts, and association rules. Do not hallucinate or invent imaginary numbers, brands, or customer names.
2. STRUCTURE: Format your output with markdown headers (## Executive Takeaway, ### Analytical Findings, ### Actionable Merchandising Recommendations).
3. ACCURACY: Always quote actual revenue numbers, percentages, lift metrics, and cluster attributes from the context.
4. TONE: Professional, concise, executive, and commercially savvy.
```

### 4.2 Scrubbed Telemetry Context Schema
```json
{
  "workspaceName": "Green Basket Supermarket",
  "totalRevenue": 4865000.00,
  "totalTransactions": 11250,
  "averageTransactionValue": 432.44,
  "grossMarginPercentage": 18.3,
  "customerClusters": [
    {"label": "High-Value Champions", "memberCount": 128},
    {"label": "Loyal Frequent Shoppers", "memberCount": 342},
    {"label": "Emerging Regulars", "memberCount": 510},
    {"label": "At-Risk Occasionals", "memberCount": 180}
  ],
  "topMarketBasketRules": [
    {"antecedent": "Atta 10kg", "consequent": "Pure Ghee 1L", "lift": "2.85", "confidence": "48.2%"},
    {"antecedent": "Bread 400g", "consequent": "Table Butter 500g", "lift": "2.40", "confidence": "52.0%"}
  ],
  "forecastDays": 14,
  "projectedRevenueNextPeriod": 6811000.00,
  "baselineRevenueNextPeriod": 6500000.00,
  "criticalStockoutRiskProducts": [
    {"productName": "Basmati Rice 5kg", "category": "Groceries", "demandClass": "HIGH", "projected7dUnits": 420.0}
  ]
}
```

---

## 5. Gemini Interactive BI Chat Copilot

### 5.1 Endpoint Specification
- **URL:** `POST /analytics/ai-insights/chat`
- **Headers:** `Content-Type: application/json`
- **Request Body:**
  ```json
  { "message": "Which product bundles should we promote this weekend?" }
  ```
- **Response Body:**
  ```json
  {
    "success": true,
    "reply": "Based on verified checkout basket telemetry, prioritize co-locating [Atta 10kg] and [Pure Ghee 1L] (Lift: 2.85x, Confidence: 48.2%) near aisle end-caps with a 10% bundle voucher...",
    "modelVersion": "gemini-2.5-flash",
    "error": null
  }
  ```

### 5.2 Read-Only Safety
The Copilot strictly queries persisted analytical caches and models. It cannot perform write mutations, drop tables, modify pricing, or alter user accounts.
