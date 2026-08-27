# Database Design

## 1. Design Approach

The database design is derived from the single-period response contracts in [APIDesign.md](APIDesign.md), but API DTOs are not copied directly into database tables.

Growth, Profitability, Liquidity, and Capital are frontend response groupings. They are assembled by backend services and should not become four structurally similar database tables.

The design uses:

- One reporting-period table.
- One source-document table.
- One metrics table for metric identity, category, and unit metadata.
- One dimensions table for scalar and breakdown members.
- One central metric-value table for scalar and breakdown values.
- One strategic-target table.
- Staging tables for AI extraction and validation.

Each metric has one definition containing its stable code, display name, category, and unit. Metric values reference this definition instead of repeating those attributes.

## 2. Relationship Overview

~~~text
metrics ───────────────< metric_values >──── reporting_periods
dimensions ────────────<       │
                              ├────────────────────── source_documents
                              │                              │
                              │                              └──< ingestion_runs
                              │                                      │
                              │                                      └──< extraction_items
                              │
                              └── optional extraction origin

metrics ───────────────< strategic_targets >──── source_documents
dimensions ────────────<        │
                               └──────────────────── reporting_periods
~~~

## 3. Core Tables

### 3.1 reporting_periods

Stores the periods returned by the reporting-period API.

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| code | VARCHAR(20) | Unique, for example FY2025 or HY2026 |
| label | VARCHAR(100) | Frontend label |
| period_type | VARCHAR(20) | FULL_YEAR or HALF_YEAR |
| start_date | DATE | Required |
| end_date | DATE | Required |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

The code must be unique, and the start date must not be later than the end date. If the API needs a default period, the service should select it through application configuration or by choosing the latest available period.

### 3.2 source_documents

Stores the provenance returned inside each API metric value.

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| name | VARCHAR(255) | Required |
| document_type | VARCHAR(50) | Financial statements, interim results, website release, or another source type |
| publication_date | DATE | Nullable when unknown |
| source_url | VARCHAR(1000) | Nullable |
| local_path | VARCHAR(1000) | Nullable |
| file_hash | VARCHAR(128) | Detects duplicate source files |
| created_at | TIMESTAMP | Required |

The API source object is created from this table together with metric_values.source_page.

### 3.3 metrics

Stores stable metadata shared by every value of the same metric.

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| code | VARCHAR(60) | Unique, stable API and service identifier |
| name | VARCHAR(150) | Human-readable metric name |
| category | VARCHAR(30) | GROWTH, PROFITABILITY, LIQUIDITY, or CAPITAL |
| unit | VARCHAR(30) | EUR, PERCENT, PERCENTAGE_POINT, COUNT, or RATIO |
| description | TEXT | Optional definition of the metric |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

Example definitions:

| code | name | category | unit |
|---|---|---|---|
| REVENUE | Revenue | GROWTH | EUR |
| GROSS_MARGIN | Gross Margin | PROFITABILITY | PERCENT |
| CASH_BALANCE | Cash Balance | LIQUIDITY | EUR |
| BANK_DEBT | Bank Debt | CAPITAL | EUR |

The category defines which category service normally returns the metric. The unit is joined into the API Metric Value response.

### 3.4 dimensions

Stores the available scalar and breakdown members.

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| dimension_type | VARCHAR(30) | TOTAL, CUSTOMER_SEGMENT, SOLUTION, GEOGRAPHY, or PIPELINE_STAGE |
| code | VARCHAR(80) | Stable member code |
| label | VARCHAR(120) | Frontend label |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

Recommended unique key:

~~~text
(dimension_type, code)
~~~

TOTAL is stored as a normal row and is used by every scalar metric.

| dimension_type | code | label |
|---|---|---|
| TOTAL | TOTAL | Total |
| CUSTOMER_SEGMENT | ENTERPRISE | Enterprise |
| CUSTOMER_SEGMENT | INDEPENDENT | Independent |
| CUSTOMER_SEGMENT | RND | R&D |
| SOLUTION | SOIL | Soil |
| SOLUTION | TERRAIN | Terrain |
| SOLUTION | ERA | ERA |
| GEOGRAPHY | IRELAND | Ireland |
| PIPELINE_STAGE | OPEN | Open |
| PIPELINE_STAGE | CLOSED | Closed |

### 3.5 metric_values

Stores all validated values used by the four category APIs.

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| period_id | BIGINT | Foreign key to reporting_periods |
| metric_id | BIGINT | Foreign key to metrics |
| dimension_id | BIGINT | Foreign key to dimensions; required |
| value | DECIMAL(20,4) | Required numeric value |
| value_status | VARCHAR(20) | REPORTED, CALCULATED, or ESTIMATED |
| value_date | DATE | Optional exact date for a point-in-time value or event |
| source_document_id | BIGINT | Foreign key to source_documents |
| source_page | INT | Nullable for a webpage source |
| comments | TEXT | Formula for calculated values or assumptions for estimated values |
| extraction_item_id | BIGINT | Optional extraction origin |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

Recommended unique key:

~~~text
(period_id, metric_id, dimension_id)
~~~

Scalar values reference the TOTAL row in dimensions. Every row in metric_values is formal, validated data and can be used by the public data APIs.

### 3.6 Dimension Use

The current APIs require one breakdown axis at a time:

- Customer segment
- Solution
- Geography
- Pipeline stage

The dimension_id foreign key supports these structures without repeating dimension type, code, and label in metric_values.

| Period | Metric | Dimension type | Dimension code | Value |
|---|---|---|---|---:|
| FY2025 | REVENUE | TOTAL | TOTAL | 836991 |
| FY2025 | CUSTOMER_COUNT | CUSTOMER_SEGMENT | ENTERPRISE | 36 |
| FY2025 | REVENUE_MIX | CUSTOMER_SEGMENT | ENTERPRISE | 69 |
| FY2025 | REVENUE_MIX | GEOGRAPHY | IRELAND | 78 |
| FY2025 | ACV | SOLUTION | ERA | 58900 |
| HY2026 | PIPELINE_VALUE | PIPELINE_STAGE | OPEN | 500000 |

## 4. Calculation Metadata

Calculated and estimated values are stored directly in metric_values. No separate calculation relationship table is required.

The comments column is used according to value_status:

- REPORTED: optional supporting context.
- CALCULATED: the calculation formula, including the metric names used as inputs.
- ESTIMATED: the estimation basis and assumptions.

For example, a calculated free cash flow value can store `Operating cash flow - Capital expenditure` in comments. This keeps the Entity and schema simple while keeping the displayed value explainable.

## 5. Strategic Targets

### 5.1 strategic_targets

A target is stored separately because it is not an observation for the selected reporting period.

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| metric_id | BIGINT | Foreign key to metrics |
| dimension_id | BIGINT | Foreign key to dimensions; use TOTAL for scalar targets |
| label | VARCHAR(150) | Frontend label |
| operator | VARCHAR(30) | GREATER_THAN_OR_EQUAL or LESS_THAN |
| target_value | DECIMAL(20,4) | Required |
| target_period_id | BIGINT | Foreign key to reporting_periods |
| source_document_id | BIGINT | Foreign key to source_documents |
| source_page | INT | Nullable |
| created_at | TIMESTAMP | Required |
| updated_at | TIMESTAMP | Required |

The Capital service uses strategic_targets.metric_id and dimension_id to obtain the actual value from metric_values for the requested period. The target API code, metric name, category, and unit come from metrics.

Recommended unique key:

~~~text
(metric_id, dimension_id, target_period_id)
~~~

## 6. AI Extraction Staging

AI output should not be written directly into metric_values as formal data.

### 6.1 ingestion_runs

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| source_document_id | BIGINT | Foreign key to source_documents |
| model_name | VARCHAR(100) | AI model used for extraction |
| status | VARCHAR(20) | RUNNING, COMPLETED, or FAILED |
| started_at | TIMESTAMP | Required |
| completed_at | TIMESTAMP | Nullable |
| error_message | TEXT | Nullable |

### 6.2 extraction_items

| Column | Type | Rule |
|---|---|---|
| id | BIGINT | Primary key |
| ingestion_run_id | BIGINT | Foreign key to ingestion_runs |
| period_code | VARCHAR(20) | Extracted period candidate |
| metric_code | VARCHAR(60) | Extracted metric code candidate |
| raw_value | VARCHAR(255) | Original extracted representation |
| numeric_value | DECIMAL(20,4) | Parsed candidate value |
| unit | VARCHAR(30) | Candidate unit |
| dimension_id | BIGINT | Foreign key to dimensions; may be nullable while pending |
| source_page | INT | Source page |
| source_text | TEXT | Supporting text excerpt |
| confidence | DECIMAL(5,4) | Extraction confidence |
| validation_status | VARCHAR(20) | PENDING, VERIFIED, or REJECTED |
| created_at | TIMESTAMP | Required |

Before an extraction item can become VERIFIED, dimension_id must reference a valid dimensions row, including TOTAL for a scalar value. An accepted item is then converted into a metric_values row, and metric_values.extraction_item_id preserves its origin.

## 7. Java Model

Recommended Entities:

- ReportingPeriodEntity
- SourceDocumentEntity
- MetricEntity
- DimensionEntity
- MetricValueEntity
- StrategicTargetEntity
- IngestionRunEntity
- ExtractionItemEntity

Recommended enums:

- PeriodType
- MetricCategory
- ValueStatus
- MetricUnit
- DimensionType
- ValidationStatus
- TargetOperator
- IngestionStatus

MetricEntity supplies stable codes such as REVENUE, GROSS_PROFIT, and CASH_BALANCE. MetricCategory and MetricUnit remain Java enums stored as strings in metrics.

JPA associations should be unidirectional and lazy by default. Controllers should return DTOs rather than Entities.

## 8. API Mapping

### 8.1 Growth

| API field | Database query |
|---|---|
| revenue | REVENUE with TOTAL/TOTAL |
| customers.total | CUSTOMER_COUNT with TOTAL/TOTAL |
| customers.enterpriseTotal | CUSTOMER_COUNT with CUSTOMER_SEGMENT/ENTERPRISE |
| customers.byCustomerSegment | CUSTOMER_COUNT grouped by CUSTOMER_SEGMENT |
| revenueMix.byCustomerSegment | REVENUE_MIX grouped by CUSTOMER_SEGMENT |
| revenueMix.bySolution | REVENUE_MIX grouped by SOLUTION |
| revenueMix.byGeography | REVENUE_MIX grouped by GEOGRAPHY |
| acvBySolution | ACV grouped by SOLUTION |
| salesPipeline.closedValue | PIPELINE_VALUE with PIPELINE_STAGE/CLOSED |
| salesPipeline.openValue | PIPELINE_VALUE with PIPELINE_STAGE/OPEN |
| salesPipeline.enterpriseCustomers | PIPELINE_CUSTOMER_COUNT with CUSTOMER_SEGMENT/ENTERPRISE |

### 8.2 Profitability

The service queries scalar TOTAL/TOTAL rows for:

- GROSS_PROFIT
- GROSS_MARGIN
- OPERATING_LOSS
- OPERATING_MARGIN
- COST_OF_SALES
- ADMINISTRATIVE_EXPENSES
- RND_INTENSITY

### 8.3 Liquidity

The service queries scalar TOTAL/TOTAL rows for:

- CASH_BALANCE
- OPERATING_CASH_FLOW
- FREE_CASH_FLOW
- WORKING_CAPITAL_MOVEMENT
- CURRENT_ASSETS
- CURRENT_LIABILITIES
- NET_CURRENT_POSITION
- CAPITAL_EXPENDITURE

The free cash flow bridge is assembled from operating cash flow, capital expenditure, and free cash flow rows for the same period.

### 8.4 Capital

The service queries scalar TOTAL/TOTAL rows for:

- BANK_DEBT
- LOAN_MOVEMENT
- INTEREST_EXPENSE
- EQUITY_FINANCING
- NET_CASH
- NET_ASSET_POSITION
- CONTINGENT_CONSIDERATION

Strategic target responses combine strategic_targets records with matching actual values from metric_values.

## 9. Indexes and Validation

Recommended indexes:

~~~text
reporting_periods(code)
metrics(code)
metrics(category)
dimensions(dimension_type, code)
metric_values(period_id, metric_id)
metric_values(period_id, metric_id, dimension_id)
metric_values(source_document_id)
strategic_targets(metric_id, dimension_id, target_period_id)
extraction_items(ingestion_run_id, validation_status)
~~~

Application validation must enforce:

- A reported value has a source document.
- A calculated value has a formula in comments.
- An estimated value has its estimation basis and assumptions in comments.
- An extracted unit matches the unit in metrics before validation.
- Every metric_values row references a valid dimensions row.
- Only VERIFIED extraction_items can be promoted into metric_values.
- Full-year and half-year comparisons are handled by the frontend using equivalent period types.

## 10. Design Outcome

This design keeps storage independent from presentation while supporting every field in the four single-period APIs. It avoids duplicate category tables, centralises metric metadata in metrics, centralises breakdown members in dimensions, keeps calculation comments in metric_values, and allows new metrics or dimensions to be added without changing the central value table structure.
