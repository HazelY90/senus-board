# AI Extraction Job Design

## 1. Purpose

The AI extraction job is a one-time, non-web process that reads configured source documents, extracts and analyses candidate financial data, and stores the results for review.

AI output must never be written directly to `metric_values`. Candidate data is first stored in `extraction_items` with a `PENDING` validation status. Only verified items may be promoted to formal metric values.

The job reuses the backend entities, repositories, datasource, and transaction management, but it does not remain active with the Spring web application.

## 2. Runtime Model

The ingestion process runs as a dedicated Spring Boot command-line job with the `ingestion` profile.

~~~text
Start non-web Spring context
        |
        v
Validate configuration and source inputs
        |
        v
Create or reuse source document
        |
        v
Create RUNNING ingestion run
        |
        v
Read and prepare document content
        |
        v
Call OpenAI outside a database transaction
        |
        v
Normalise and validate candidate output
        |
        v
Store PENDING extraction items
        |
        v
Mark ingestion run COMPLETED
        |
        v
Exit process
~~~

Database migrations remain a separate deployment or local-development step. The ingestion profile therefore disables Flyway and the web server.

## 3. Package Structure

The job implementation is located under:

~~~text
com.hazely.senusboard.jobs.ingestion
~~~

### 3.1 IngestionJob

Defines the configuration boundary for the one-time job. It is responsible for activating ingestion-only beans and configuration without starting the web layer.

It must not contain document parsing, AI request, or persistence logic.

### 3.2 IngestionRunner

Coordinates one complete job execution. Its responsibilities are:

- Read job arguments.
- Validate required configuration.
- Invoke the extraction workflow.
- Convert failures into a non-zero process exit status.
- Allow the application context to close when execution finishes.

### 3.3 ExtractionService

Orchestrates extraction for each source document. Its responsibilities are:

- Detect the source document by file hash.
- Create a new `ingestion_runs` record with `RUNNING` status.
- Read and prepare document content.
- Call `AiClient` outside long-running database transactions.
- Pass provider output to `AnalysisService`.
- Persist candidate values as `PENDING` extraction items.
- Mark the ingestion run as `COMPLETED` or `FAILED`.

### 3.4 AnalysisService

Normalises and validates provider output before review. Its responsibilities are:

- Validate the structured response shape.
- Parse numeric values into `BigDecimal`.
- Normalise metric, unit, period, and dimension codes.
- Validate page numbers and confidence values.
- Preserve supporting source text.
- Reject incomplete or structurally invalid candidates.

This service must not write unverified data to `metric_values`.

### 3.5 AiClient

Defines the provider boundary for AI requests. Its responsibilities are:

- Build structured extraction requests.
- Send document content and extraction instructions to OpenAI.
- Request a predictable structured response.
- Return provider-neutral result objects to the service layer.
- Expose request failures without performing database operations.

OpenAI-specific request and response types should remain behind this interface.

### 3.6 PromotionService

Converts verified extraction items into formal metric values. Its responsibilities are:

- Require `VERIFIED` validation status.
- Resolve existing reporting period, metric, dimension, and source records.
- Confirm that the extracted unit matches the metric unit.
- Create or update the matching `metric_values` row.
- Set `extraction_item_id` to preserve the extraction origin.
- Perform promotion in one short transaction.
- Remain idempotent when promotion is requested more than once.

The service must resolve shared records by their unique codes instead of creating duplicate reference rows.

### 3.7 IngestionProperties

Holds external job configuration. Planned settings include:

- Source document locations.
- OpenAI model selection.
- Request limits and timeouts.
- Retry limits.
- Job feature switches.

Sensitive settings must come from environment variables or a deployment secret manager and must never be logged.

## 4. Review Boundary

`ExtractionReviewService` belongs to the regular backend service layer rather than the one-time job package. It supports the review workflow while the web application is running.

The review workflow is:

1. Load `PENDING` extraction items.
2. Compare each candidate with its source page and supporting text.
3. Mark valid items as `VERIFIED`.
4. Mark invalid items as `REJECTED`.
5. Invoke `PromotionService` only for verified items.

If review is later automated, the same validation and promotion rules must still be applied.

## 5. Database State Transitions

### 5.1 Ingestion Run

~~~text
RUNNING -> COMPLETED
RUNNING -> FAILED
~~~

- `started_at` is set when the run is created.
- `completed_at` is set when the job succeeds or fails.
- `error_message` contains a safe failure summary for a failed run.
- Secrets, full request headers, and sensitive provider responses must not be stored in `error_message`.

### 5.2 Extraction Item

~~~text
PENDING -> VERIFIED -> promoted to metric_values
PENDING -> REJECTED
~~~

A verified item remains available after promotion so the formal value retains traceability through `metric_values.extraction_item_id`.

## 6. Transaction Boundaries

External document and AI operations must not hold an open database transaction.

Recommended transaction boundaries are:

1. Create or resolve the source document and create the ingestion run, then commit.
2. Read files and call OpenAI without a database transaction.
3. Persist analysed extraction items and mark the run completed in a short transaction.
4. Mark the run failed in a separate short transaction when processing fails.
5. Review an extraction item in a short transaction.
6. Promote one verified item or one controlled batch in a short transaction.

## 7. Idempotency

The job must be safe to retry.

- Use `source_documents.file_hash` to detect an already known source file.
- Do not call OpenAI again when a completed extraction run already represents the same source and extraction configuration unless an explicit force option is supplied.
- Use the unique metric-value key `(period_id, metric_id, dimension_id)` to prevent duplicate formal values.
- Check for an existing metric value before promotion and apply an explicit update policy.
- Preserve the extraction item ID used for each promoted value.
- Do not retry invalid configuration or schema-validation failures automatically.

## 8. Failure Handling

Failures are divided into the following categories:

- Configuration failure: stop before creating an ingestion run when possible.
- Source failure: mark the run failed when a configured document cannot be read.
- Provider failure: record a safe summary and return a non-zero exit status.
- Response validation failure: preserve valid diagnostics without promoting partial output.
- Persistence failure: roll back the affected short transaction and mark the run failed in a separate transaction.
- Promotion conflict: leave the item verified, report the conflict, and do not overwrite formal data implicitly.

Retries should use bounded attempts with backoff and should apply only to transient provider or network failures.

## 9. Configuration

The ingestion profile is defined in `application-ingestion.yaml`:

~~~yaml
spring:
  main:
    web-application-type: none

  flyway:
    enabled: false

app:
  openai:
    api-key: ${OPENAI_API_KEY}

  job:
    ingestion:
      enabled: true
~~~

Local development stores `OPENAI_API_KEY` in the ignored `.env` file. Cloud execution must inject it through the platform secret manager. The key must not be committed, printed, or included in failure messages.

Model selection is intentionally not fixed yet. It should become an external configuration value when the OpenAI client is implemented.

## 10. Planned Execution

After the job classes are implemented, the intended execution shape is:

~~~bash
java -jar backend.jar \
  --spring.profiles.active=ingestion \
  --spring.main.web-application-type=none
~~~

The process must exit with code `0` only after all configured documents finish successfully. Any incomplete or failed run must produce a non-zero exit code.

## 11. Security and Data Handling

- Keep API keys only in server-side environment variables or secret managers.
- Send only the source content required for extraction.
- Never include credentials in prompts, logs, database fields, or exception messages.
- Record the model name in `ingestion_runs.model_name` for auditability.
- Preserve page numbers and supporting source text for review.
- Treat AI output as untrusted input until it passes structural validation and review.
