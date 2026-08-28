# Database Design

## 1. Design Goal

The schema uses fixed category columns to provide a stable output shape across FY2024, FY2025, HY2025, and HY2026.

The reference tables are:

- `reporting_periods`.
- `source_documents`.

`ingestion_runs` remains an operational audit table linked to `source_documents`.

The four reporting tables are:

- `growth`.
- `profitability`.
- `liquidity`.
- `capital`.

Four calculated tables store deterministic values derived from the matching frontend categories:

- `calculated_growth`.
- `calculated_profitability`.
- `calculated_liquidity`.
- `calculated_capital`.

## 2. Relationships

~~~text
source_documents ──< ingestion_runs

reporting_periods ── growth
                  ├─ calculated_growth
                  ├─ profitability
                  ├─ calculated_profitability
                  ├─ liquidity
                  ├─ calculated_liquidity
                  ├─ capital
                  ├─ calculated_capital
                  └─ analytics
~~~

Each reporting and calculated table contains a unique `reporting_period_id` foreign key. Therefore, each reporting period has at most one row in each table.

## 3. Reference Tables

### 3.1 reporting_periods

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| code | VARCHAR(20) | Unique and required |
| label | VARCHAR(100) | Required |
| period_type | VARCHAR(20) | FULL_YEAR or HALF_YEAR |
| start_date | DATE | Required |
| end_date | DATE | Required |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

### 3.2 source_documents

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| name | VARCHAR(255) | Required |
| document_type | VARCHAR(50) | Required |
| publication_date | DATE | Nullable |
| source_url | VARCHAR(1000) | Nullable |
| local_path | VARCHAR(1000) | Nullable path relative to the application working directory |
| file_hash | VARCHAR(128) | Unique and required |
| ai_summary | TEXT | Nullable AI summary of the source document |
| created_at | TIMESTAMP | Required |

## 4. Category Tables

All financial fields use `DECIMAL(20,4)` and allow null values. Every category table also includes `id`, `created_at`, and `updated_at`.

### 4.1 growth

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| revenue | Nullable |

### 4.2 profitability

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| gross_profit | Nullable |
| gross_margin | Nullable |
| operating_loss | Nullable |
| cost_of_sales | Nullable |
| administrative_expenses | Nullable |

### 4.3 liquidity

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| cash_balance | Nullable |
| operating_cash_flow | Nullable |
| working_capital_movement | Nullable |
| current_assets | Nullable |
| current_liabilities | Nullable |
| net_current_position | Nullable |
| capital_expenditure | Nullable |

### 4.4 capital

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| bank_debt | Nullable |
| loan_movement | Nullable |
| interest_expense | Nullable |
| net_asset_position | Nullable |

### 4.5 calculated_growth

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| revenue_growth | Nullable percentage |

### 4.6 calculated_profitability

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| calculated_gross_margin | Nullable percentage |
| operating_margin | Nullable percentage |
| cost_of_sales_ratio | Nullable percentage |
| administrative_expense_ratio | Nullable percentage |

### 4.7 calculated_liquidity

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| operating_cash_flow_margin | Nullable percentage |
| free_cash_flow | Nullable EUR value |
| free_cash_flow_margin | Nullable percentage |
| current_ratio | Nullable ratio |
| cash_ratio | Nullable ratio |

### 4.8 calculated_capital

| Column | Rule |
|---|---|
| reporting_period_id | Unique foreign key to reporting_periods |
| net_cash | Nullable EUR value |

Every calculated table also includes `id`, `created_at`, and `updated_at`.

~~~text
revenue_growth = (current revenue / equivalent prior revenue - 1) * 100
calculated_gross_margin = gross_profit / revenue * 100
operating_margin = operating_loss / revenue * 100
cost_of_sales_ratio = abs(cost_of_sales) / revenue * 100
administrative_expense_ratio = abs(administrative_expenses) / revenue * 100
operating_cash_flow_margin = operating_cash_flow / revenue * 100
free_cash_flow = operating_cash_flow + capital_expenditure
free_cash_flow_margin = free_cash_flow / revenue * 100
current_ratio = current_assets / abs(current_liabilities)
cash_ratio = cash_balance / abs(current_liabilities)
net_cash = cash_balance - bank_debt
~~~

Revenue growth uses the previous equivalent reporting period. Full-year values use the prior full year, and half-year values use the equivalent prior half year.

## 5. AI Analysis Table

### 5.1 analytics

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| reporting_period_id | BIGINT | Unique foreign key to reporting_periods |
| growth_analytics | TEXT | Nullable |
| profitability_analytics | TEXT | Nullable |
| liquidity_analytics | TEXT | Nullable |
| capital_analytics | TEXT | Nullable |
| total_analytics | TEXT | Nullable |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

Each row contains AI analysis for one reporting period. Category columns align directly with the four frontend categories. `total_analytics` combines the available reported and calculated values across all four categories.

## 6. Write Rules

- Align exact extracted date ranges to backend-controlled canonical period codes, labels, and types.
- Map July through June to the ending-year full-year period and July through December to the following-year half-year period.
- Skip a date range that does not exactly match a supported canonical boundary.
- Resolve the aligned reporting period by its unique canonical code.
- Create or update one category row for that period.
- Apply later non-null values to the existing category row and retain an existing field when the later extraction returns null for that field.
- Reject duplicate period codes in one extraction response.
- Reject a period whose start date follows its end date.
- Reject a period that contains no reported category value.
- Persist only the primary document period and formal immediately preceding comparative period selected by the extraction rules.
- Recalculate the affected calculated category rows after any reported category row changes.
- Keep a calculated field null when an input is null or a denominator is zero.
- Never copy a calculated value into a reported category column.
- Store the extraction summary in `source_documents.ai_summary`.
- Generate `analytics` only after all reported and calculated rows for the ingestion batch are committed.
- Upsert one `analytics` row for each reporting period returned by the analysis response.
- Keep an analysis field null when its category has insufficient data.
- Do not roll back reported or calculated rows when AI analysis fails.
