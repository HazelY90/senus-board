# Development Plan

## 1. Requirements and Source Validation

- Validate FY2024, FY2025, HY2025, and HY2026 source values.
- Retain only fields that are sufficiently stable across annual and half-year disclosures.
- Record excluded metrics and the reason for exclusion in [Requirements.md](Requirements.md).

## 2. Design Alignment

- Define the four fixed frontend categories in [FrontendDesign.md](FrontendDesign.md).
- Define one complete single-period API response in [APIDesign.md](APIDesign.md).
- Define the period-based category schema in [DatabaseDesign.md](DatabaseDesign.md).
- Define direct fixed-schema extraction in [AIExtractionJob.md](AIExtractionJob.md).

## 3. Database Work

- Use `reporting_periods` and `source_documents` as reference tables.
- Use `ingestion_runs` as source-level operational audit data.
- Add nullable `ai_summary` to `source_documents`.
- Create `growth`, `profitability`, `liquidity`, and `capital`.
- Create `calculated_growth`, `calculated_profitability`, `calculated_liquidity`, and `calculated_capital`.
- Add a unique `reporting_period_id` foreign key to every reported and calculated category table.
- Create `analytics` with one unique `reporting_period_id` row per period.

## 4. Backend Work

- Define fixed period category extraction objects.
- Add primary-period and formal-comparative-period selection rules.
- Add tests that ignore incidental historical dates and values.
- Add entities and repositories for the four category tables.
- Upsert category rows directly during ingestion.
- Apply later non-null values while retaining existing fields when a later extraction returns null.
- Add a period-based query service and one complete period data controller.
- Add a source-document list endpoint with metadata and server-hosted download links.
- Add an ID-based source-document download endpoint without exposing local filesystem paths.
- Add a calculation service with explicit formulas, null handling, denominator checks, and comparable-period resolution.
- Store a source-level AI summary during document extraction.
- Add a complete-dataset AI analysis request after all extraction and calculation work commits.
- Validate and upsert period-level `analytics` rows without rolling back stored numeric data on analysis failure.
- Add validation and repository tests.

## 5. Frontend Work

- Implement reporting-period selection.
- Load all four categories and AI analysis from one complete period response.
- Implement the four category views.
- Compare only equivalent period types.
- Handle null values without converting them to zero.
- Add responsive cards, paired comparison charts, and detail tables.
- Display category and total AI analysis with an explicit AI-generated label.
- Display source-document metadata, AI summaries, and available download actions.

## 6. Deployment Work

- Back up the target database before schema deployment.
- Deploy the database schema separately from ingestion.
- Run ingestion after schema validation succeeds.
- Verify the complete period endpoint against extracted, calculated, and AI analysis values.
- Verify document listing, unavailable-file handling, safe filenames, and local downloads.
