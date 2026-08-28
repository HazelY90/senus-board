
## 1. Data Sources

SenusBoard will use the Senus investor relations website as the primary source registry:

- Senus Investor Relations: https://app.assiduous.tech/investor-relations/senus
- Financial documents published through the investor relations website, including the Information Document, annual results, half-year results, investor presentations, and future disclosures
- The Euronext Senus page may be used as a secondary source for company and market-reference information

Financial reporting is document- and event-driven. Each source document must be stored or registered separately with its document type, publication date, reporting period, source URL, and ingestion status. New reports must append new reporting periods rather than overwrite historical records.

Extracted financial facts are stored directly in the fixed reporting-period category tables. Individual values do not retain field-level source, page, extraction-method, confidence, or validation-status metadata. Figures identified in the initial project summary must be verified against the original disclosures before they are included in the supported fixed schema. Social media must not be used as a primary financial source.

## 2. Target Users

The platform is intended for:

- Management
- The Board
- Equity Investors
- Credit Providers

The application should provide an executive-level view that allows these users to analyse and understand Senus PLC's financial and operating performance. It should resemble a credible platform that a CEO could use, rather than a detailed accounting system.

## 3. Report Content and User Priorities

| Financial Metric | Primary Audience | Provided by Data Sources |
| --- | --- | --- |
| Revenue and revenue trend | Management, Board, Equity Investors | Yes |
| Year-on-year revenue growth | Management, Board, Equity Investors | Derived from available annual revenue |
| Month-on-month revenue growth | Management | No confirmed monthly data |
| Customer accounts and customer mix | Management, Board, Equity Investors | Yes, subject to source verification |
| Revenue mix by customer or product category | Management, Board, Equity Investors | Yes, subject to source verification |
| Geographic revenue mix | Management, Board, Equity Investors | Yes, subject to source verification |
| Bookings and customer segments | Management, Board, Equity Investors | No confirmed data |
| Gross profit and gross margin | Management, Board, Equity Investors | Gross profit is available; margin is derived |
| Operating profit or loss and operating margin | Management, Board, Equity Investors | Operating result is available; margin is derived |
| EBITDA and EBITDA margin | Management, Board, Equity Investors, Credit Providers | To verify |
| Operating cost breakdown and R&D intensity | Management, Board, Equity Investors | Partial; subject to source verification |
| Cash balance | Management, Board, Credit Providers | Yes |
| Operating cash flow | Management, Board, Credit Providers | Yes |
| Free cash flow and EBITDA-to-free-cash-flow bridge | Management, Board, Equity Investors, Credit Providers | To verify |
| Working capital | Management, Board, Credit Providers | To verify from balance-sheet disclosures |
| Cash runway | Management, Board, Credit Providers | Derived only when sufficiently current cash and burn data are available |
| Debt and leverage | Board, Equity Investors, Credit Providers | To verify, including subsequent financing events |
| Debt-service coverage ratio | Board, Credit Providers | Derived only when debt-service inputs are available |
| ROCE | Board, Equity Investors | Derived only when capital-employed inputs are available |
| Senus 2030 target progress | Management, Board, Equity Investors | Strategic targets are available; actual progress is derived |

The application must not invent unavailable metrics. A metric should be displayed only when it is present in the supported fixed schema and all inputs required for its calculation are available. Corporate events and subsequent financing must be considered before conclusions are drawn from historic period-end figures. AI-generated commentary must use only stored reported values and deterministically calculated metrics.

## 4. Data Source Validation

Validation completed on 27 August 2026 using the [Senus investor relations page](https://app.assiduous.tech/investor-relations/senus), which redirects to the current Assiduous-hosted site, and the [Senus Euronext page](https://live.euronext.com/en/product/equities/IE000O0F49R3-XACD). The principal source documents are the [December 2025 Information Document](https://live.euronext.com/sites/default/files/2025-12/SENUS%20PLC%20-%20Information%20Document.pdf) and the unaudited HY2026 results published on the investor relations page.

| Financial Metric | Validation Result | Evidence and Limitation |
| --- | --- | --- |
| Revenue and revenue trend | **Provided** | FY2024 revenue was €688,317 and FY2025 revenue was €836,991. HY2025 revenue was €340,931 and HY2026 revenue was €354,813. Annual and half-year comparisons are available. |
| Year-on-year revenue growth | **Provided and derivable** | The Information Document reports FY2025 growth of 21.6%. The HY2026 results report growth of 4.1% against HY2025. The values can also be recalculated from reported revenue. |
| Month-on-month revenue growth | **Not provided** | No monthly revenue series was found. Commentary mentions seasonality and activity in the final two months of 2025, but this is not sufficient for a monthly growth calculation. |
| Customer accounts and customer mix | **Provided for FY2025** | The Information Document reports 138 accounts: 36 Enterprise, 98 Independent, and 4 R&D customers. HY2026 reports 21 enterprise customers associated with closed pipeline deals, but does not provide an updated total customer mix. |
| Revenue mix by customer or product category | **Provided for FY2025** | Revenue was split 69% Enterprise, 4% Independent, and 27% R&D. The Information Document also provides revenue by solution and customer segment. No equivalent HY2026 mix was found. |
| Geographic revenue mix | **Provided for FY2025** | Ireland represented approximately 78% of FY2025 revenue. The Information Document also provides Ireland, UK, and rest-of-world percentages by solution. No equivalent HY2026 geographic split was found. |
| Bookings, customer segments, and pipeline | **Partially provided** | Enterprise, Independent, and R&D customer segments are defined and quantified. HY2026 discloses approximately €700k of closed pipeline deals across 21 enterprise customers, approximately €500k of open pipeline, and 10 deals worth approximately €425k closed in the final two months of 2025. These pipeline figures should not be labelled as accounting bookings without a defined bookings policy. |
| Gross profit and gross margin | **Provided** | FY2025 gross profit was €648,450 with a 77.5% margin, compared with €432,477 and 62.8% in FY2024. HY2026 gross profit was €289,952 with an 81.7% margin, compared with €272,331 and 79.8% in HY2025. |
| Operating profit or loss and operating margin | **Result provided; margin derivable** | Operating losses were €1,130,729 in FY2024, €633,694 in FY2025, €405,577 in HY2025, and €483,753 in HY2026. Operating margin is not stated but can be calculated from revenue and operating result. |
| EBITDA and EBITDA margin | **Not provided historically** | The sources provide an FY2028 EBITDA-positive target but no historical EBITDA or EBITDA margin. These metrics require a documented reconciliation before use. |
| Operating cost breakdown and R&D intensity | **Partially provided** | Cost of sales and administrative expenses are reported. The Information Document states R&D expenditure of approximately 22% of FY2024 revenue and 17% of FY2025 revenue. A more detailed recurring cost breakdown is not consistently available for every period. |
| Cash balance | **Provided** | Closing cash was €424,639 in FY2024, €140,135 in FY2025, €72,382 in HY2025, and €735,189 in HY2026. The HY2026 balance reflects the €1.1m equity raise and must not be compared without financing context. |
| Operating cash flow | **Provided** | Net cash used in operating activities was €1,166,697 in FY2024, €374,820 in FY2025, €450,181 in HY2025, and €410,291 in HY2026. |
| Free cash flow and EBITDA-to-free-cash-flow bridge | **Partially derivable** | Operating and investing cash flows are provided. HY2026 separately reports €8,500 paid to acquire tangible or intangible assets, allowing free cash flow to be derived under a documented definition. An EBITDA-to-free-cash-flow bridge cannot be produced because historical EBITDA is not provided. |
| Working capital | **Provided** | HY2026 reports a €64,839 positive movement in working capital, current assets of €923,339, current creditors of €387,105, contingent consideration of €850,000, and net current liabilities of €313,766. FY2025 also provides debtor and creditor movements and qualitative working-capital commentary. |
| Cash runway | **Conditionally derivable, not directly provided** | Cash and operating cash burn are available, but any runway estimate would be sensitive to seasonality, financing, acquisition effects, contingent consideration, investment plans, and the age of the reporting date. It should be labelled as an estimate with explicit assumptions, not as a validated reported metric. |
| Debt and leverage | **Debt provided; leverage not provided** | HY2026 reports bank debt of €76,474. FY2025 discloses a new €100,000 term loan and other financing activity. No published net-debt-to-EBITDA or comparable leverage ratio was found. |
| Debt-service coverage ratio | **Not currently derivable reliably** | Interest expense is reported, but scheduled principal repayments and a suitable positive debt-service cash-flow measure are not fully disclosed. Historical operating results are negative. |
| ROCE | **Not directly provided** | Operating results and balance-sheet values are available, but a consistent capital-employed definition and appropriate opening or average balances are not provided for every period. ROCE should remain unavailable until the calculation policy and inputs are validated. |
| Senus 2030 target progress | **Provided and derivable** | Published targets include revenue CAGR of at least 50% through FY2030, more than 100 Enterprise customers, Enterprise ACV above €50,000, less than 50% of revenue from Ireland, and EBITDA positivity during FY2028. FY2025 actuals provide baselines for progress comparisons. |

The validated reporting periods currently available are FY2024, FY2025, HY2025, and HY2026. Annual and half-year figures must not be placed in the same trend series without clear period labels or annualisation rules. Reported values, deterministically derived metrics, estimates, and unavailable metrics must be visually distinguished in the application.

## 5. Data to Display

All four user types will have access to the same complete set of supported metrics. The metrics are divided into four non-overlapping data categories. The selected user type changes only the default category order and emphasis; it does not hide data or create a different dataset.

Every value must be labelled as `Reported`, `Calculated`, or `Estimated`, and must retain its reporting period and source reference. An `Estimated` value must also show its assumptions, calculation method, and calculation date. Annual and half-year data must remain clearly separated.

### 5.1 Growth and Revenue

| Metric to Display | Data Basis | Intended Use |
| --- | --- | --- |
| Revenue and revenue trend | Reported for FY2024, FY2025, HY2025, and HY2026 | Show business scale and changes over time. |
| Year-on-year revenue growth | Reported and recalculable from comparable revenue periods | Measure the pace of growth against prior-year performance. |
| Customer accounts and customer mix | Reported for FY2025 | Show the size and composition of the customer base. |
| Enterprise customer count | Reported for FY2025 | Track adoption by higher-value customers and progress toward the FY2030 target. |
| Revenue mix by customer segment and solution | Reported for FY2025 | Show which customer segments and solutions generate revenue. |
| Geographic revenue mix | Reported for FY2025 | Track international expansion and geographic concentration. |
| Enterprise ACV by solution | Reported for FY2025 | Show contract value by solution and progress toward the Enterprise ACV target. |
| Customer segments and pipeline | Reported for FY2025 and HY2026 | Show Enterprise, Independent, and R&D customer segments together with closed and open pipeline. Pipeline must not be labelled as accounting bookings. |

### 5.2 Profitability and Efficiency

| Metric to Display | Data Basis | Intended Use |
| --- | --- | --- |
| Gross profit | Reported for annual and half-year comparative periods | Show the amount remaining after direct costs. |
| Gross margin | Reported and recalculable from revenue and gross profit | Measure pricing and direct-cost efficiency. |
| Operating loss trend | Reported for annual and half-year comparative periods | Show whether operating losses are improving or worsening. |
| Operating margin | Calculated from reported operating result and revenue | Measure operating performance relative to revenue. |
| Cost of sales | Reported for annual and half-year comparative periods | Show the direct cost required to deliver revenue. |
| Administrative expenses | Reported for annual and half-year comparative periods | Show the principal disclosed operating expense category. |
| R&D intensity | Reported for FY2024 and FY2025 | Show the proportion of revenue invested in research and development. |

### 5.3 Cash and Liquidity

| Metric to Display | Data Basis | Intended Use |
| --- | --- | --- |
| Cash balance | Reported for annual and half-year comparative periods | Show immediately available liquidity. |
| Operating cash flow | Reported for annual and half-year comparative periods | Show whether operations generate or consume cash. |
| Free cash flow | Calculated from reported operating cash flow and capital expenditure where available | Estimate cash remaining after operating and investment requirements under a documented calculation policy. |
| Working-capital movement | Reported in FY2025 and HY2026 disclosures | Show the cash effect of changes in operating assets and liabilities. |
| Current assets | Reported in HY2026 and the underlying financial statements | Show short-term assets available to support operations. |
| Current liabilities | Reported in HY2026 and the underlying financial statements | Show obligations expected to affect short-term liquidity. |
| Net current assets or liabilities | Reported in HY2026 and derivable from current assets and current liabilities | Summarise the short-term liquidity position. |

### 5.4 Capital, Solvency, and Strategic Returns

| Metric to Display | Data Basis | Intended Use |
| --- | --- | --- |
| Bank debt | Reported for HY2026 | Show outstanding disclosed bank borrowing. |
| Loan movements | Reported in annual and half-year cash-flow statements | Show changes in debt financing. |
| Interest expense | Reported for HY2025 and HY2026 | Show the disclosed cost of debt. |
| Equity financing | Reported in HY2026 and related disclosures | Explain changes in liquidity and capital structure caused by the €1.1m equity raise. |
| Net cash position | Calculated from reported cash and bank debt | Provide a simple solvency and liquidity measure without an unsupported EBITDA multiple. |
| Net assets or liabilities | Reported for annual and half-year comparative periods | Show the balance-sheet position attributable to the company. |
| Contingent consideration | Reported in HY2026 | Show the potential acquisition-related obligation associated with Loamin. |
| Senus 2030 target progress | Reported strategic targets compared with reported actuals | Show progress toward revenue growth, Enterprise customer, ACV, geographic, and profitability targets. |

### 5.5 Default Display Order by User Type

All categories remain available in navigation. The application changes the landing-page order and visual emphasis according to the selected user type.

| User Type | 1st Priority | 2nd Priority | 3rd Priority | 4th Priority |
| --- | --- | --- | --- | --- |
| Management | Growth and Revenue | Profitability and Efficiency | Cash and Liquidity | Capital, Solvency, and Strategic Returns |
| Board | Capital, Solvency, and Strategic Returns | Cash and Liquidity | Growth and Revenue | Profitability and Efficiency |
| Equity Investors | Capital, Solvency, and Strategic Returns | Growth and Revenue | Profitability and Efficiency | Cash and Liquidity |
| Credit Providers | Cash and Liquidity | Capital, Solvency, and Strategic Returns | Profitability and Efficiency | Growth and Revenue |

The initial product must exclude month-on-month revenue growth, historical EBITDA, EBITDA margin, a full EBITDA-to-free-cash-flow bridge, debt-service coverage ratio, ROCE, and cash runway because the currently published data do not support reliable calculations. These metrics may be added later when the required source data and calculation policies are available.

## 6. Final Fixed-Schema Design

The source analysis above remains the complete record of available and unavailable data. The initial database implementation intentionally uses a narrower subset so that every category has a stable response shape across FY2024, FY2025, HY2025, and HY2026.

An individual value may occasionally be unavailable. In that case, the field remains null and the other values for the same period remain valid.

### 6.1 Reporting Period Scope

| Code | Type | Comparable Period |
| --- | --- | --- |
| FY2024 | FULL_YEAR | FY2025 |
| FY2025 | FULL_YEAR | FY2024 |
| HY2025 | HALF_YEAR | HY2026 |
| HY2026 | HALF_YEAR | HY2025 |

Full-year and half-year performance values must not be compared directly. Point-in-time balances must retain their applicable reporting date.

### 6.2 Fixed Growth Fields

| Column | Unit | Selection Basis |
| --- | --- | --- |
| revenue | EUR | Consistently reported for all four validated periods |

Customer counts, customer mix, revenue mix, ACV, geography, and pipeline remain part of the source analysis but are excluded from the initial fixed schema because equivalent values are not consistently available for every period.

### 6.3 Fixed Profitability Fields

| Column | Unit | Selection Basis |
| --- | --- | --- |
| gross_profit | EUR | Consistently reported for annual and half-year periods |
| gross_margin | PERCENT | Reported in results summaries; may occasionally be absent |
| operating_loss | EUR | Consistently reported for annual and half-year periods |
| cost_of_sales | EUR | Consistently reported for annual and half-year periods |
| administrative_expenses | EUR | Consistently reported for annual and half-year periods |

Operating margin and R&D intensity remain excluded from the initial fixed schema because they are derived or unavailable for some periods.

### 6.4 Fixed Liquidity Fields

| Column | Unit | Selection Basis |
| --- | --- | --- |
| cash_balance | EUR | Consistently reported closing balance |
| operating_cash_flow | EUR | Consistently reported for annual and half-year periods |
| working_capital_movement | EUR | Available in cash-flow disclosures; may occasionally be absent |
| current_assets | EUR | Consistently available from balance sheets |
| current_liabilities | EUR | Consistently available from balance sheets |
| net_current_position | EUR | Consistently available from balance sheets |
| capital_expenditure | EUR | Available in cash-flow disclosures; may occasionally be absent |

Free cash flow remains part of the broader product analysis but is excluded from the stored fixed schema because it requires a calculation policy.

### 6.5 Fixed Capital Fields

| Column | Unit | Selection Basis |
| --- | --- | --- |
| bank_debt | EUR | Available when debt is outstanding; may be absent or zero in earlier periods |
| loan_movement | EUR | Available in financing cash flows; may occasionally be absent |
| interest_expense | EUR | Reported across annual and half-year periods |
| net_asset_position | EUR | Consistently available from balance sheets |

Equity financing, net cash, contingent consideration, and Senus 2030 target progress remain documented above but are excluded from the initial fixed schema because they are event-specific, calculated, or not comparable across all periods.

### 6.6 Storage and Missing-Value Rules

- Retain `reporting_periods` and `source_documents`.
- Store reporting values in `growth`, `profitability`, `liquidity`, and `capital`.
- Give every category table a unique `reporting_period_id` foreign key.
- Use fixed nullable columns instead of metric, metric-value, or dimension rows.
- Store monetary values in EUR base units.
- Store displayed percentages directly, so 81.7% is stored as 81.7.
- Preserve accounting signs for deductions, losses, and values shown in parentheses.
- Prefer consolidated or group values when both group and company values are available.
- Keep an unavailable value null; never replace it with zero.
- When the same reporting period is extracted again, update existing category fields with later non-null values and retain the existing value wherever the later value is null.

### 6.7 Deferred Scope

The complete source analysis remains available for later expansion. Excluded metrics may be introduced only after equivalent definitions and sufficiently stable values become available across comparable periods. Adding them will require an explicit schema and API design update.

### 6.8 Calculated Metrics

Four calculated tables store deterministic results derived from the fixed reported fields:

- `calculated_growth`.
- `calculated_profitability`.
- `calculated_liquidity`.
- `calculated_capital`.

Each calculated table has one row per reporting period and uses a unique `reporting_period_id` foreign key. This keeps calculated values aligned with the four frontend categories while preserving their separation from reported values.

This final decision supersedes the earlier initial-product exclusion for free cash flow and net cash. They are now included only as deterministic calculated fields and remain distinct from reported source values.

| Table | Column | Formula | Required Inputs |
| --- | --- | --- | --- |
| calculated_growth | revenue_growth | `(current revenue / comparable prior revenue - 1) * 100` | Current and equivalent prior-period revenue |
| calculated_profitability | calculated_gross_margin | `gross_profit / revenue * 100` | Gross profit and revenue |
| calculated_profitability | operating_margin | `operating_loss / revenue * 100` | Operating loss and revenue |
| calculated_profitability | cost_of_sales_ratio | `abs(cost_of_sales) / revenue * 100` | Cost of sales and revenue |
| calculated_profitability | administrative_expense_ratio | `abs(administrative_expenses) / revenue * 100` | Administrative expenses and revenue |
| calculated_liquidity | operating_cash_flow_margin | `operating_cash_flow / revenue * 100` | Operating cash flow and revenue |
| calculated_liquidity | free_cash_flow | `operating_cash_flow + capital_expenditure` | Operating cash flow and signed capital expenditure |
| calculated_liquidity | free_cash_flow_margin | `free_cash_flow / revenue * 100` | Free cash flow and revenue |
| calculated_liquidity | current_ratio | `current_assets / abs(current_liabilities)` | Current assets and current liabilities |
| calculated_liquidity | cash_ratio | `cash_balance / abs(current_liabilities)` | Cash balance and current liabilities |
| calculated_capital | net_cash | `cash_balance - bank_debt` | Cash balance and bank debt |

Revenue growth uses the previous equivalent period: FY2025 uses FY2024, and HY2026 uses HY2025. It remains null when an equivalent prior period is unavailable.

Capital expenditure is stored as a negative cash outflow, so free cash flow uses addition. Bank debt is stored as a positive closing balance, so net cash uses subtraction.

A calculated field remains null when any required input is null, when a denominator is zero, or when equivalent period selection is ambiguous. Calculated values must not overwrite reported fields. In particular, reported `gross_margin` remains in `profitability`, while `calculated_gross_margin` is stored in `calculated_profitability` for validation and consistent comparison.
