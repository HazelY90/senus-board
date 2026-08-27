# API Design

## 1. Scope

This document defines only the backend APIs required to supply financial and operating data to the frontend. User management, authentication, authorisation, and user preference APIs are outside the current scope.

Each category API returns data for one reporting period only. The backend does not provide separate comparison endpoints or return previous-period and change fields.

When the frontend needs a comparison, it requests the same category for two equivalent periods and compares the two single-period responses. For example:

~~~http
GET /api/v1/data/growth?period=HY2026
GET /api/v1/data/growth?period=HY2025
~~~

The frontend must compare full-year values only with full-year values and half-year values only with half-year values. Point-in-time balances may be compared when their reporting dates remain visible.

## 2. Endpoint Summary

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/v1/reporting-periods | Return periods available for frontend selection |
| GET | /api/v1/data/growth?period={periodCode} | Return Growth data for one period |
| GET | /api/v1/data/profitability?period={periodCode} | Return Profitability data for one period |
| GET | /api/v1/data/liquidity?period={periodCode} | Return Liquidity data for one period |
| GET | /api/v1/data/capital?period={periodCode} | Return Capital data for one period |

The four category endpoints can be requested independently and in parallel.

## 3. Common Response Objects

### 3.1 Reporting Period

~~~jsonc
{
  "code": "HY2026",
  "label": "Half Year 2026",
  "type": "HALF_YEAR",
  "startDate": "2025-07-01",
  "endDate": "2025-12-31"
}
~~~

Supported period types are FULL_YEAR and HALF_YEAR. Additional period types should be added only when the source data supports them.

### 3.2 Metric Value

Every financial or operating value returned by a category API must use this structure:

~~~jsonc
{
  "value": 354813,
  "status": "REPORTED",
  "unit": "EUR",
  "comments": null,
  "source": {
    "documentId": 12,
    "documentName": "HY2026 Interim Results",
    "page": 4,
    "url": null
  }
}
~~~

Allowed status values are:

- REPORTED
- CALCULATED
- ESTIMATED

Initial units include:

- EUR
- PERCENT
- PERCENTAGE_POINT
- COUNT
- RATIO

A CALCULATED value requires its formula in comments. An ESTIMATED value requires its estimation basis and assumptions in comments. Unavailable data must be omitted or explicitly identified as unavailable; it must not be returned as zero.

If the frontend calculates a change between two API responses, the displayed change must be labelled as Calculated.

## 4. Reporting Periods API

### GET /api/v1/reporting-periods

Returns the reporting periods that can be selected by the frontend.

Response template:

~~~jsonc
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

The frontend uses period type to prevent invalid full-year and half-year comparisons.

## 5. Growth Data API

### GET /api/v1/data/growth?period={periodCode}

Returns Growth data for one selected reporting period.

Response template:

~~~jsonc
{
  "period": {},
  "revenue": {},
  "customers": {
    "total": {},
    "enterpriseTotal": {},
    "byCustomerSegment": [
      {
        "segment": "ENTERPRISE | INDEPENDENT | RND",
        "metric": {}
      }
    ]
  },
  "revenueMix": {
    "byCustomerSegment": [
      {
        "code": "ENTERPRISE",
        "label": "Enterprise",
        "metric": {}
      }
    ],
    "bySolution": [
      {
        "code": "ERA",
        "label": "ERA",
        "metric": {}
      }
    ],
    "byGeography": [
      {
        "code": "IRELAND",
        "label": "Ireland",
        "metric": {}
      }
    ]
  },
  "acvBySolution": [
    {
      "solution": "SOIL | TERRAIN | ERA",
      "metric": {}
    }
  ],
  "salesPipeline": {
    "closedValue": {},
    "openValue": {},
    "enterpriseCustomers": {}
  }
}
~~~

The period field is a Reporting Period object. Every metric field is a Metric Value object.

Required returned data:

- Revenue.
- Total customer count, enterprise customer count, and customer mix.
- Revenue mix by each supported customer segment, solution, and geography.
- ACV by solution.
- Closed pipeline, open pipeline, and related enterprise customer count.

Revenue growth is not returned. The frontend calculates it from revenue values obtained from two equivalent-period responses.

## 6. Profitability Data API

### GET /api/v1/data/profitability?period={periodCode}

Returns Profitability data for one selected reporting period.

Response template:

~~~jsonc
{
  "period": {},
  "grossProfit": {},
  "grossMargin": {},
  "operatingLoss": {},
  "operatingMargin": {},
  "costOfSales": {},
  "administrativeExpenses": {},
  "rndIntensity": {}
}
~~~

Required returned data:

- Gross profit.
- Gross margin.
- Operating loss.
- Operating margin.
- Cost of sales.
- Administrative expenses.
- R&D intensity.

The frontend can compare a metric only when both selected periods return the same field with compatible units and definitions.

## 7. Liquidity Data API

### GET /api/v1/data/liquidity?period={periodCode}

Returns Liquidity data for one selected reporting period.

Response template:

~~~jsonc
{
  "period": {},
  "cashBalance": {},
  "operatingCashFlow": {},
  "freeCashFlow": {},
  "workingCapitalMovement": {},
  "currentAssets": {},
  "currentLiabilities": {},
  "netCurrentPosition": {},
  "freeCashFlowBridge": {
    "operatingCashFlow": {},
    "capitalExpenditure": {},
    "freeCashFlow": {}
  }
}
~~~

Required returned data:

- Cash balance.
- Operating cash flow.
- Free cash flow.
- Working capital movement.
- Current assets.
- Current liabilities.
- Net current position.
- Inputs and result for the free cash flow calculation bridge.

A cash balance is a point-in-time value. The frontend must retain the reporting dates from both period objects when comparing two balances.

## 8. Capital Data API

### GET /api/v1/data/capital?period={periodCode}

Returns Capital data for one selected reporting period.

Response template:

~~~jsonc
{
  "period": {},
  "bankDebt": {},
  "loanMovement": {},
  "interestExpense": {},
  "equityFinancing": {},
  "netCash": {},
  "netAssetPosition": {},
  "contingentConsideration": {},
  "strategicTargets": [
    {
      "code": "REVENUE_CAGR",
      "label": "Revenue CAGR",
      "actual": {},
      "target": {},
      "operator": "GREATER_THAN_OR_EQUAL | LESS_THAN",
      "targetPeriod": "FY2030"
    }
  ]
}
~~~

Required returned data:

- Bank debt and loan movements.
- Interest expense.
- Equity financing.
- Net cash.
- Net asset position.
- Contingent consideration.
- Actual and target values for each supported Senus 2030 strategic measure.

Strategic target actual and target values remain in this response because they represent target assessment rather than reporting-period comparison.

## 9. Response Rules

- Each category endpoint returns one reporting period only.
- Category responses must not contain previous-period or period-change fields.
- The frontend requests two single-period responses when it needs a comparison.
- Income statement and cash-flow comparisons must use equivalent period types.
- Point-in-time comparisons must preserve exact reporting dates.
- Every returned value must include its unit, status, and source.
- Calculations and estimates must remain explainable from the response.
- Unsupported fields should be omitted or explicitly marked unavailable; they must never be returned as zero.
- API DTOs and database entities do not need a one-to-one structure.
