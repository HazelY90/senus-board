# Frontend Design

## 1. Design Approach

The dashboard presents four fixed category views from each complete period response:

- Growth.
- Profitability.
- Liquidity.
- Capital.

The available history supports paired period comparisons rather than long trend lines. The frontend requests one complete period dataset containing all four categories and AI analysis. It requests a second equivalent period dataset when comparison is needed.

## 2. Common Display Rules

- Compare FY2024 with FY2025.
- Compare HY2025 with HY2026.
- Do not compare a full year with a half year.
- Display the period end date for balance-sheet values.
- Hide a missing optional visual or show `Unavailable`; never display a missing value as zero.
- Use EUR formatting for monetary values and percentage formatting for gross margin.
- Preserve negative accounting signs in cards, charts, and tables.

## 3. Growth

Growth contains revenue only.

The page should use:

- A revenue KPI card for the selected period.
- A paired bar chart for the selected period and its equivalent prior period.
- A calculated percentage change displayed by the frontend when both revenue values are present.

Revenue growth may be loaded from `calculated_growth` after the backend resolves the equivalent prior period. It must be labelled as calculated.

## 4. Profitability

The page should show:

- Gross profit.
- Gross margin.
- Operating loss.
- Cost of sales.
- Administrative expenses.

Gross profit, gross margin, and operating loss are the headline cards. A paired bar chart may compare gross profit and operating loss. Cost of sales and administrative expenses belong in a compact detail table.

A larger operating loss must not be styled as a positive improvement.

Calculated gross margin, operating margin, cost-of-sales ratio, and administrative-expense ratio may appear in the detail table. Reported gross margin and calculated gross margin must use distinct labels when both are shown.

## 5. Liquidity

The page should show:

- Cash balance.
- Operating cash flow.
- Working capital movement.
- Current assets.
- Current liabilities.
- Net current position.
- Capital expenditure.

Cash balance, operating cash flow, and net current position are the headline cards. Current assets, current liabilities, and net current position should use a balance-sheet detail table. Working capital movement and capital expenditure may be omitted from a comparison visual when either period is null.

Calculated operating-cash-flow margin, free cash flow, free-cash-flow margin, current ratio, and cash ratio may appear below the reported liquidity values. Every calculated value must expose its formula and remain unavailable when an input is missing.

## 6. Capital

The page should show:

- Bank debt.
- Loan movement.
- Interest expense.
- Net asset position.

Bank debt and net asset position are the headline cards. Loan movement and interest expense should appear in a financing detail table. A missing bank debt value must be shown as unavailable unless the source explicitly reports zero debt.

Net cash may be displayed as a calculated KPI when both cash balance and bank debt are available.

## 7. Responsive Layout

Each category follows the same layout:

1. Two or three headline KPI cards.
2. One paired-period comparison chart when comparable values exist.
3. One detailed table containing every available field.

On narrow screens, KPI cards and comparison bars stack vertically. Tables may scroll horizontally but must keep metric labels visible.

## 8. User-Type Ordering

| User type | First | Second | Third | Fourth |
|---|---|---|---|---|
| Management | Growth | Profitability | Liquidity | Capital |
| Board | Capital | Liquidity | Growth | Profitability |
| Equity Investors | Capital | Growth | Profitability | Liquidity |
| Credit Providers | Liquidity | Capital | Profitability | Growth |

All categories remain accessible regardless of the selected user type.

## 9. AI Analysis

The frontend reads `analytics` from the complete `/api/v1/data?period={code}` response after loading the selected period.

- Growth displays `growthAnalytics`.
- Profitability displays `profitabilityAnalytics`.
- Liquidity displays `liquidityAnalytics`.
- Capital displays `capitalAnalytics`.
- The period overview displays `totalAnalytics`.

Analysis text must be labelled as AI-generated. A null analysis field is omitted or shown as unavailable. AI analysis must not be styled as a reported fact, calculated metric, forecast, or recommendation.

## 10. Source Documents

The frontend loads the source-document list from `GET /api/v1/data/documents`.

Each document entry displays:

- Document name.
- Document type.
- Publication date, or `Unavailable` when it is null.
- AI summary, or `Unavailable` when it is null.
- A download action when `downloadUrl` is not null.

The download action uses the server-provided URL without constructing or exposing a filesystem path. Documents appear in the API order, with the newest publication date first.
