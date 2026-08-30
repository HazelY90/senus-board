# API Design

## 1. Scope

The financial data API is reporting-period based. One data request returns the complete reported, calculated, and AI analysis dataset for one selected period. A comparison request identifies two periods through query parameters and returns their stored AI comparison analysis. A separate document endpoint returns source-document metadata and server-hosted download links.

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
| GET | `/api/v1/data/comparisons?basePeriod={base}&targetPeriod={target}` | Return stored analysis comparing two periods |
| GET | `/api/v1/data/documents` | Return source-document metadata and download links |
| GET | `/api/v1/data/documents/{id}/download` | Download one locally stored source document |

The comparison endpoint uses GET because it is read-only. Both period codes are query parameters and neither period code appears in the path or request body.

~~~http
GET /api/v1/data/comparisons?basePeriod=FY2024&targetPeriod=FY2025
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

### GET `/api/v1/data/comparisons`

Request:

~~~http
GET /api/v1/data/comparisons?basePeriod=FY2024&targetPeriod=FY2025
~~~

Response:

~~~json
{
  "basePeriod": {
    "code": "FY2024",
    "label": "Full Year 2024",
    "type": "FULL_YEAR",
    "startDate": "2023-07-01",
    "endDate": "2024-06-30"
  },
  "targetPeriod": {
    "code": "FY2025",
    "label": "Full Year 2025",
    "type": "FULL_YEAR",
    "startDate": "2024-07-01",
    "endDate": "2025-06-30"
  },
  "analytics": {
    "growthAnalytics": "Revenue increased from FY2024 to FY2025.",
    "profitabilityAnalytics": "Gross margin improved while the operating loss remained material.",
    "liquidityAnalytics": "Operating cash outflow improved, while closing cash declined.",
    "capitalAnalytics": "Bank debt increased and the target period retained positive net cash.",
    "totalAnalytics": "Growth and margin improved, while losses and negative operating cash flow remained material."
  }
}
~~~

- Compare FY2024 only with FY2025.
- Compare HY2025 only with HY2026.
- Do not compare full-year performance values with half-year performance values.
- `basePeriod` and `targetPeriod` are required non-blank canonical period codes.
- The ordered pair in the request must match a supported stored comparison.
- The base period must end before the target period.
- Point-in-time balances retain each response's exact period end date.
- Treat null as unavailable and never as zero.
- Preserve the meaning of negative losses, expenses, and cash flows.
- Avoid claims based on ignored historical references or excluded metrics.
- Return null for a category analysis when the available data are insufficient.
- Avoid recommendations, forecasts, or unsupported causal claims.
- Every comparison analytics field is a JSON string or null.
- A missing period or comparison returns HTTP 404.
- A blank code, identical period pair, reversed pair, unsupported pair, or mismatched period type returns HTTP 400.
- The endpoint reads stored comparison analysis and does not call the AI provider during the request.

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
