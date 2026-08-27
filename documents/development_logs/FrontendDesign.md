# Frontend Design

## 1. Design Approach

The dashboard should prioritise clear current-period performance and direct period-on-period comparison. The available financial history is limited, so KPI cards, paired comparisons, composition charts, calculation bridges, and detail tables are more appropriate than conventional long-term line charts.

The frontend should request the four data categories independently:

- Growth
- Profitability
- Liquidity
- Capital

All user groups can access all four categories. The frontend changes only their display order according to the selected user type.

Each category should follow a consistent page structure:

1. Three or four headline KPI cards.
2. One primary comparison or composition chart.
3. One supporting visual where it adds useful context.
4. A detailed data table for exact values, value status, and source access.

## 2. Display Format Selection

| Data characteristic | Recommended format | Example |
|---|---|---|
| A single current value | KPI card | Cash balance or bank debt |
| Two comparable periods | Paired bar chart or comparison card | HY2025 revenue versus HY2026 revenue |
| Current value, previous value, and change | KPI comparison card | Revenue with year-on-year growth |
| Several business categories | Horizontal bar chart | ACV by solution |
| Percentage composition | 100% stacked bar chart | Revenue mix |
| Actual value against a target | Target indicator or progress bar | Enterprise customer target |
| A calculation relationship | Waterfall chart or calculation detail | Operating cash flow less capital expenditure equals free cash flow |
| Several detailed values | Data table | Current assets and current liabilities |
| Data origin and value classification | Badge and detail popover | Reported, Calculated, or Estimated |

Every displayed value must be labelled as `Reported`, `Calculated`, or `Estimated`. A `Calculated` value should expose its formula and inputs. An `Estimated` value should expose its method, assumptions, and calculation date.

## 3. Growth

### 3.1 Headline KPI Cards

The initial cards should show:

- Revenue
- Revenue growth
- Customer count
- Open sales pipeline

Each card should include the current value, the comparable period where available, the change, and the value classification.

### 3.2 Revenue Comparison

Revenue should use a paired bar chart rather than a line chart. Full-year and half-year comparisons must be kept separate:

- FY2024 versus FY2025
- HY2025 versus HY2026

The four periods must not be connected as one continuous series because full-year and half-year values use different reporting durations.

### 3.3 Revenue and Customer Mix

Revenue mix should use a 100% stacked bar chart. This format makes the Enterprise, Independent, and R&D proportions easy to compare and works well on smaller screens.

Customer mix should use a horizontal bar chart to compare customer counts across the same categories.

### 3.4 ACV by Solution

ACV for Soil, Terrain, and ERA should use a horizontal bar chart. This is a category comparison and does not require a long time series.

### 3.5 Pipeline

Closed and open pipeline values should appear as KPI cards or a segmented horizontal bar. The visual must distinguish completed sales from potential sales and must not imply that the open pipeline is recognised revenue.

## 4. Profitability

### 4.1 Headline KPI Cards

The initial cards should show:

- Gross profit
- Gross margin
- Operating loss
- R&D intensity

### 4.2 Comparable-Period Charts

Gross margin and operating loss can use paired bar charts for FY2024 versus FY2025 or HY2025 versus HY2026. Operating loss should use clear labels and colour treatment so that a larger loss is not presented as a positive result.

### 4.3 Detailed Profitability Table

The table should contain:

| Metric | Current period | Comparable period | Change | Value status |
|---|---:|---:|---:|---|
| Revenue | Value | Value | Percentage change | Reported |
| Gross profit | Value | Value | Percentage change | Reported |
| Gross margin | Value | Value | Percentage-point change | Calculated |
| Operating loss | Value | Value | Percentage change | Reported |

The table provides exact values while the chart provides a faster visual comparison. A separate chart is not required for every metric.

## 5. Liquidity

### 5.1 Headline KPI Cards

The initial cards should show:

- Cash balance
- Operating cash flow
- Free cash flow
- Net current position

### 5.2 Free Cash Flow Calculation

A waterfall chart can explain the free cash flow calculation:

```text
Operating cash flow - Capital expenditure = Free cash flow
```

This visual explains a calculation relationship and therefore remains useful without a long time series.

### 5.3 Liquidity Detail

Current assets, current liabilities, and the net current position should use a compact table or horizontal comparison bars. A table is preferable when accounting adjustments or contingent consideration need to be explained precisely.

Cash balance may be shown across several reporting dates, but each point must be labelled with its exact reporting date because it is a point-in-time balance.

## 6. Capital

### 6.1 Headline KPI Cards

The initial cards should show:

- Bank debt
- Equity financing
- Net cash
- Contingent consideration

### 6.2 Strategic Targets

Senus 2030 targets should use target indicators or progress bars where progress can be interpreted meaningfully. Suitable examples include enterprise customer count and Enterprise ACV.

Revenue CAGR should show the actual value beside the target rather than treating `actual / target` as completion progress. A growth-rate target is not a cumulative task and a simple completion percentage could be misleading.

### 6.3 Financing Detail

A table should show bank debt, equity financing, interest expense, net cash, and contingent consideration together with period and value status.

## 7. Reporting-Period Rules

The frontend must apply the following comparison rules:

- Compare FY2024 only with FY2025.
- Compare HY2025 only with HY2026.
- Do not connect full-year and half-year income or cash-flow values in one continuous trend line.
- Point-in-time balance sheet values may be displayed by reporting date, provided that the dates and period types remain visible.
- Percentage-point changes and percentage changes must be labelled differently.

The available history is sufficient for paired comparisons, composition charts, category charts, target indicators, calculation visuals, KPI cards, and detailed tables. It is not sufficient to support strong claims about long-term trends.

## 8. Recommended Visual Balance

The page should use approximately:

- 60% KPI cards
- 25% charts
- 15% detailed tables

This balance keeps the dashboard easy to scan while preserving access to exact values and their provenance. The design should emphasise current performance and comparable-period movement rather than creating an artificial long-term trend from a small number of observations.

The data endpoints and response templates required by this frontend design are defined in [APIDesign.md](APIDesign.md).
