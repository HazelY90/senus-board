# AI Extraction Job Design

## 1. Purpose

The ingestion job discovers source documents, downloads them, extracts a fixed financial schema and source summary, validates reporting periods, writes the reported and calculated category tables, and then requests AI analysis of individual periods and supported period comparisons.

AI extracts reported fields only. Deterministic values are calculated by application code after the reported category rows are stored.

## 2. Runtime Flow

~~~text
Start the non-web Spring context
        |
        v
Validate job and OpenAI configuration
        |
        v
Discover and download source documents
        |
        v
For each document:
    create or reuse source_documents metadata
        |
        v
    create a RUNNING ingestion_runs row
        |
        v
    upload the document and request strict JSON output
        |
        v
    delete the uploaded provider file
        |
        v
    validate reporting periods and fixed category fields
        |
        v
    upsert reporting_periods and category rows
        |
        v
    recalculate affected calculated category rows
        |
        v
    mark the run COMPLETED
        |
        v
After every document is complete:
    load the complete reported and calculated dataset
        |
        v
    submit the dataset for strict structured AI analysis
        |
        v
    validate and upsert analytics rows by reporting period
        |
        v
    build supported ordered comparison pairs
        |
        v
    calculate deterministic metric changes
        |
        v
    submit comparison inputs for strict structured AI analysis
        |
        v
    validate and upsert comparison_analytics rows
        |
        v
Exit
~~~

Documents are processed sequentially. A document failure stops the remaining work and marks its ingestion run as FAILED.

## 3. Structured Output

The provider returns:

~~~json
{
  "publicationDate": "2026-03-19",
  "aiSummary": "Unaudited half-year results covering HY2026 with HY2025 comparative figures.",
  "periods": [
    {
      "startDate": "2025-07-01",
      "endDate": "2025-12-31",
      "growth": {},
      "profitability": {},
      "liquidity": {},
      "capital": {}
    }
  ]
}
~~~

Each category object has a fixed field set matching the category table. Individual values and complete category objects may be null.

`aiSummary` contains a concise source-document summary and is stored in `source_documents.ai_summary`.

The `periods` array contains only the document's primary reporting period and any formal immediately preceding comparative period. Incidental historical references do not create period objects.

The provider returns exact date ranges without period identity fields. The backend aligns supported boundaries to canonical periods: July through June becomes the ending-year `FYyyyy`, and July through December becomes the following-year `HYyyyy`. Unsupported ranges are skipped.

## 4. Extraction Rules

- Identify the document's primary reporting period from its title, results heading, main financial-statement headings, and stated period end date.
- Extract the primary reporting period when it contains at least one supported fixed metric.
- Extract an immediately preceding period only when it is a formal parallel comparative column in the main financial statements or main results table.
- Inspect the complete primary statements and their formal comparative columns.
- Ignore isolated references to older reports, older financial years, opening balances, event dates, acquisition dates, strategy baselines, and historical examples.
- Ignore a historical value that appears only in narrative text, a note, a footnote, or a chart annotation unless the same period is a formal main comparative column.
- Do not treat the publication date, document creation date, event date, or target date as a reporting period.
- Do not return a period that contains no supported fixed metric values.
- Return exact start and end dates for the supported annual and half-year documents.
- Extract consolidated or group figures when both group and company figures are present.
- Convert thousands and millions to EUR base units.
- Return displayed percentages without dividing by 100.
- Preserve accounting signs.
- Return null when a value is missing or ambiguous.
- Do not invent, derive, or rename fields.
- Return only fields defined by the fixed extraction schema.

## 5. Persistence Transactions

### 5.1 Start

The start transaction:

- Calculates the file SHA-256 hash.
- Creates or updates `source_documents` metadata.
- Creates a `RUNNING` ingestion run with the configured model and start time.

### 5.2 Complete

The completion transaction:

- Requires the ingestion run to be RUNNING.
- Validates the optional publication date.
- Validates and stores the non-blank source-document AI summary.
- Derives canonical period codes, labels, and types from exact supported date boundaries.
- Rejects duplicate canonical periods in one extraction response.
- Validates period type and dates.
- Rejects a period whose category objects contain no fixed metric value.
- Creates or updates each reporting period by its backend-derived canonical code.
- Skips unsupported date ranges without modifying a canonical reporting period.
- Upserts each non-null category object by `reporting_period_id`.
- Marks the ingestion run COMPLETED.

Any validation or persistence error rolls back the completion transaction.

After reported category rows are written, the same transaction recalculates `calculated_growth`, `calculated_profitability`, `calculated_liquidity`, and `calculated_capital` as required. Calculations use decimal arithmetic and explicit rounding rules. A missing input or zero denominator produces null rather than an exception or zero result.

### 5.3 Fail

The failure transaction records the completion time, a concise error summary, and FAILED status.

## 6. Complete-Dataset Analysis

After every document in the ingestion batch has completed, the job loads all available reporting periods together with their reported and calculated category values. It serializes this complete dataset as the input to a separate AI analysis request.

The analysis response uses a strict schema:

~~~json
{
  "periods": [
    {
      "periodCode": "HY2026",
      "growthAnalytics": "Revenue increased against the equivalent half-year period.",
      "profitabilityAnalytics": "Gross margin improved while operating loss increased.",
      "liquidityAnalytics": "Closing cash increased, while operating cash flow remained negative.",
      "capitalAnalytics": "The period closed with positive net cash based on reported cash and bank debt.",
      "totalAnalytics": "Growth and margin improved, but operating losses and negative operating cash flow remain material."
    }
  ]
}
~~~

The backend validates that every returned period code exists in `reporting_periods`, rejects duplicate period codes, and upserts one `analytics` row per period.

The analysis prompt requires the model to use only supplied reported and calculated values, keep null values unavailable, compare equivalent period types only, distinguish calculated values from reported values, and avoid forecasts, recommendations, or unsupported causes.

The analysis request runs outside a database transaction. Its persistence step uses a separate short transaction. An analysis request or validation failure leaves all reported and calculated data unchanged.

## 7. Period Comparison Analysis

After complete-dataset analysis, the job builds supported ordered comparison pairs in application code. The initially supported pairs are FY2024 to FY2025 and HY2025 to HY2026. The model never selects or reorders the periods.

Both periods must exist, have the same period type, and satisfy `basePeriod.endDate < targetPeriod.endDate`. Full-year periods are compared only with full-year periods, and half-year periods are compared only with equivalent half-year periods.

Application code calculates available absolute changes and percentage changes before the AI request. A percentage change is null when either source value is null or the base value is zero. Negative losses, expenses, and cash flows retain their accounting meaning. The AI receives the two period datasets and the deterministic changes, and produces narrative analysis only.

Comparison analysis uses a dedicated prompt, strict JSON Schema, DTO, service, and persistence boundary while reusing the configured AI provider client. It is generated during ingestion and is never generated by the comparison API request.

The comparison response uses this strict shape:

~~~json
{
  "comparisons": [
    {
      "basePeriodCode": "FY2024",
      "targetPeriodCode": "FY2025",
      "growthAnalytics": "Revenue increased from FY2024 to FY2025.",
      "profitabilityAnalytics": "Gross margin improved while the operating loss remained material.",
      "liquidityAnalytics": "Operating cash outflow improved, while closing cash declined.",
      "capitalAnalytics": "Bank debt increased and the target period retained positive net cash.",
      "totalAnalytics": "Growth and margin improved, while losses and negative operating cash flow remained material."
    }
  ]
}
~~~

The backend validates exact period codes, rejects duplicate ordered pairs, and verifies that every returned pair was present in the submitted comparison dataset. It upserts each result by `(base_period_id, target_period_id)`.

Before requesting analysis, the job calculates a SHA-256 `input_hash` from the canonical pair input. It skips regeneration when the hash matches the stored row. A changed input regenerates and replaces the stored narrative.

The comparison request and persistence transaction are separate. A provider, schema-validation, or comparison-persistence failure leaves reported values, calculated values, and existing period analysis unchanged.

## 8. Update Policy

Every category table has a unique `reporting_period_id`. A later extraction for the same period updates the existing row.

When multiple documents report the same period, later documents take precedence field by field. A later non-null value replaces the existing value, while a later null leaves the existing value unchanged.

When a period changes, calculations for that period are refreshed. Revenue growth for a later comparable period must also be refreshed because it depends on the changed period as its comparison input.

After any successful ingestion batch changes reported or calculated values, complete-dataset analysis is regenerated so every affected `analytics` row uses the same dataset version.

Supported comparison rows affected by changed input are regenerated after period analysis. Unchanged comparisons are retained through their matching input hash.

## 9. Configuration and Security

- Run only under the ingestion profile.
- Keep Flyway disabled during extraction.
- Validate the database schema before starting the job.
- Keep the OpenAI API key outside source control.
- Restrict downloads and redirects to the configured HTTPS source host.
- Enforce file-size and normalized-path checks.
- Reuse an existing regular file when the resolved safe filename already exists in the download directory, without downloading the file again.
- Use `store: false` for provider responses.
- Attempt to delete every uploaded provider file.
- Do not log source content, provider output, API keys, or request headers.

## 10. Operational Constraints

- Documents are processed sequentially.
- A repeated file still starts a new ingestion run.
- There is no extraction-configuration fingerprint.
- There is no source-priority policy for overlapping periods.
- OpenAI transport failures use at most two retries with short exponential backoff.
- Field-level page evidence and confidence are not stored in the fixed category model.
- AI analysis adds a second provider request after extraction and calculation complete.
- Comparison analysis adds a separate provider request only when a supported comparison input has changed.
- The comparison API reads persisted results and never waits for a provider response.
- Analysis text is model-generated interpretation and must remain visibly distinct from reported and calculated values.
