# API Design

## 1. Scope

The financial data API is reporting-period based. One data request returns the complete reported, calculated, and AI analysis dataset for one selected period. A separate document endpoint returns source-document metadata and server-hosted download links.

The response always contains:

- `period`.
- `growth`.
- `profitability`.
- `liquidity`.
- `capital`.
- `analytics`.

## 2. Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/data/reporting-periods` | Return available periods |
| GET | `/api/v1/data/{period}` | Return the complete dataset for one period |
| GET | `/api/v1/data/documents` | Return source-document metadata and download links |
| GET | `/api/v1/data/documents/{id}/download` | Download one locally stored source document |

The frontend requests two complete period responses when it needs a comparison.

~~~http
GET /api/v1/data/FY2025
GET /api/v1/data/FY2024
~~~

## 3. Reporting Period Object

~~~json
{
  "code": "HY2026",
  "label": "Half Year 2026",
  "type": "HALF_YEAR",
  "startDate": "2025-07-01",
  "endDate": "2025-12-31"
}
~~~

## 4. Reporting Periods

### GET `/api/v1/data/reporting-periods`

~~~json
{
  "periods": [
    {
      "code": "HY2026",
      "label": "Half Year 2026",
      "type": "HALF_YEAR",
      "startDate": "2025-07-01",
      "endDate": "2025-12-31",
      "isDefault": true
    },
    {
      "code": "HY2025",
      "label": "Half Year 2025",
      "type": "HALF_YEAR",
      "startDate": "2024-07-01",
      "endDate": "2024-12-31",
      "isDefault": false
    }
  ]
}
~~~

## 5. Complete Period Data

### GET `/api/v1/data/FY2025`

~~~json
{
  "period": {
    "code": "FY2025",
    "label": "Full Year 2025",
    "type": "FULL_YEAR",
    "startDate": "2024-07-01",
    "endDate": "2025-06-30"
  },
  "growth": {
    "revenue": 836991.0,
    "calculated": {
      "revenueGrowth": 21.6
    }
  },
  "profitability": {
    "grossProfit": 648450.0,
    "grossMargin": 77.5,
    "operatingLoss": -633694.0,
    "costOfSales": -188541.0,
    "administrativeExpenses": -1286058.0,
    "calculated": {
      "grossMargin": 77.474,
      "operatingMargin": -75.711,
      "costOfSalesRatio": 22.526,
      "administrativeExpenseRatio": 153.664
    }
  },
  "liquidity": {
    "cashBalance": 140135.0,
    "operatingCashFlow": -374820.0,
    "workingCapitalMovement": 212467.0,
    "currentAssets": 263138.0,
    "currentLiabilities": -243846.0,
    "netCurrentPosition": 19292.0,
    "capitalExpenditure": -4451.0,
    "calculated": {
      "operatingCashFlowMargin": -44.782,
      "freeCashFlow": -379271.0,
      "freeCashFlowMargin": -45.314,
      "currentRatio": 1.079,
      "cashRatio": 0.575
    }
  },
  "capital": {
    "bankDebt": 83655.0,
    "loanMovement": 93767.0,
    "interestExpense": -2074.0,
    "netAssetPosition": -15575.0,
    "calculated": {
      "netCash": 56480.0
    }
  },
  "analytics": {
    "growthAnalytics": "Revenue increased against FY2024.",
    "profitabilityAnalytics": "Gross margin improved and operating loss decreased.",
    "liquidityAnalytics": "Operating cash outflow improved, while closing cash declined.",
    "capitalAnalytics": "The period closed with bank debt and a negative net asset position.",
    "totalAnalytics": "Revenue and gross margin improved, while losses and negative operating cash flow remained material."
  }
}
~~~

## 6. Response Rules

- One response represents exactly one reporting period.
- `period`, `growth`, `profitability`, `liquidity`, `capital`, and `analytics` are always present.
- Every category has a fixed set of fields.
- Every reported and calculated metric field is a JSON number or null.
- Missing numeric data is returned as null, never zero.
- Every category returns calculated values inside a `calculated` object, including categories with only one calculated field.
- Calculated fields are null when their required inputs are unavailable.
- Reported and calculated gross margin values remain distinguishable by their object location.
- Monetary values use EUR base units.
- Percentages use displayed percentage values.
- Accounting deductions retain negative signs.
- Every `analytics` field is a JSON string or null.
- AI analysis remains structurally separate from reported and calculated numeric values.
- A missing period returns HTTP 404.
- An invalid or blank period code returns HTTP 400.

## 7. Comparison Rules

- Compare FY2024 only with FY2025.
- Compare HY2025 only with HY2026.
- Do not compare full-year performance values with half-year performance values.
- The frontend obtains comparisons by requesting two complete period responses.
- Point-in-time balances retain each response's exact period end date.
- Treat null as unavailable and never as zero.
- Preserve the meaning of negative losses, expenses, and cash flows.
- Avoid claims based on ignored historical references or excluded metrics.
- Return null for a category analysis when the available data are insufficient.
- Avoid recommendations, forecasts, or unsupported causal claims.

An AI analysis failure must not roll back or invalidate successfully stored reported and calculated data.

## 8. Source Documents

### GET `/api/v1/data/documents`

~~~json
{
  "documents": [
    {
      "name": "Senus PLC FY2025 Annual Results.pdf",
      "type": "ANNUAL_RESULTS",
      "publicationDate": "2025-12-18",
      "aiSummary": "FY2025 annual results with FY2024 formal comparative values.",
      "downloadUrl": "/api/v1/data/documents/12/download"
    }
  ]
}
~~~

Document response rules:

- `name` comes from `source_documents.name`.
- `type` comes from `source_documents.document_type`.
- `publicationDate` and `aiSummary` are a JSON string or null.
- `downloadUrl` is a server-relative URL or null when no readable local file is available.
- The response must not expose `source_documents.local_path`.
- Documents are ordered by publication date descending, with creation time descending as the fallback order.

### GET `/api/v1/data/documents/{id}/download`

- The server resolves the file from the stored document ID and never accepts a filesystem path from the client.
- A missing document or unavailable local file returns HTTP 404.
- The response uses `Content-Disposition: attachment` with a safe filename derived from the stored document name.
- The response uses the detected media type, or `application/octet-stream` when the media type is unknown.
