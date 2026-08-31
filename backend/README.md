# SenusBoard Backend

This README covers backend configuration, startup, data ingestion, packaging, and verification. See the [project README](../README.md) and the linked design records there for product and design documentation.

## Prerequisites

- Java 25
- A running MySQL server
- An OpenAI API key only when running the ingestion job

The repository includes Maven Wrapper, so a separate Maven installation is not required.

Run every command in this README from the `backend` directory:

```bash
cd backend
```

On Windows, replace `./mvnw` with `mvnw.cmd`.

## Database

Create an empty MySQL database before starting the application. For example:

```sql
CREATE DATABASE senus_board
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

The configured database user must be able to connect to this database and apply schema migrations. A normal backend startup runs the Flyway migrations in `src/main/resources/db/migration` automatically. Hibernate then validates the mapped entities against the migrated schema.

The ingestion profile disables Flyway. Run the normal web application at least once against a new database before running ingestion.

## Environment Configuration

Create `backend/.env`. The file is ignored by Git.

```dotenv
DB_URL=jdbc:mysql://localhost:3306/senus_board
DB_USERNAME=senus_user
DB_PASSWORD=replace-with-database-password

flyway.url=jdbc:mysql://localhost:3306/senus_board
flyway.user=senus_user
flyway.password=replace-with-database-password

JWT_SECRET_KEY=replace-with-a-random-secret-of-at-least-32-bytes
BASEURL=.

ADMIN_BOOTSTRAP_ENABLED=false
ADMIN_BOOTSTRAP_EMAIL=admin@senus.ie
ADMIN_BOOTSTRAP_PASSWORD=replace-with-admin-password
ADMIN_BOOTSTRAP_NAME=Senus Admin
ADMIN_BOOTSTRAP_ORGANIZATION=Senus PLC

OPENAI_API_KEY=replace-with-openai-api-key
```

Do not commit real credentials. Process environment variables can be used instead of `.env` in deployed environments.

### Required variables

| Variable | Required for | Description |
| --- | --- | --- |
| `DB_URL` | All runs | JDBC URL for the MySQL database |
| `DB_USERNAME` | All runs | MySQL username |
| `DB_PASSWORD` | All runs | MySQL password |
| `flyway.url` | Manual Flyway commands | JDBC URL read by the Maven Flyway plugin |
| `flyway.user` | Manual Flyway commands | MySQL username read by the Maven Flyway plugin |
| `flyway.password` | Manual Flyway commands | MySQL password read by the Maven Flyway plugin |
| `JWT_SECRET_KEY` | Web application | Secret used to sign access and refresh tokens; use at least 32 random bytes |
| `BASEURL` | Web application | Base directory used to resolve stored source-document paths; use `.` when running from `backend` |
| `OPENAI_API_KEY` | Ingestion only | OpenAI API credential used for extraction and analysis |

### Initial Admin account

The four `ADMIN_BOOTSTRAP_*` identity fields are required only when `ADMIN_BOOTSTRAP_ENABLED=true`.

To create the first Admin account:

1. Set `ADMIN_BOOTSTRAP_ENABLED=true`.
2. Supply the Admin email, password, name, and organisation.
3. Start the normal web application once.
4. Confirm that the Admin can sign in.
5. Set `ADMIN_BOOTSTRAP_ENABLED=false` before later starts.

The email domain must be one of the configured enterprise domains. The Admin password must contain at least 16 characters, including uppercase, lowercase, numeric, and special characters. Bootstrap does nothing when an Admin account already exists.

## Run Database Migrations

A normal web-application startup reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` and applies pending Flyway migrations automatically.

To apply migrations without starting the web server, run:

```bash
./mvnw flyway:migrate
```

The Maven Flyway plugin reads `flyway.url`, `flyway.user`, and `flyway.password` from `backend/.env`. These values normally identify the same database and credentials as the corresponding `DB_*` variables.

Do not run the ingestion profile against a new database until the migrations have been applied through normal application startup or `flyway:migrate`.

## Run the Web Application

Start the development server:

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080` by default. The frontend should be configured to forward `/api` requests to this address.

OpenAPI endpoints are available while the backend is running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Stop the application with `Ctrl+C`.

## Run Financial Data Ingestion

Before ingestion:

- Complete the normal database migration by starting the web application at least once.
- Set `OPENAI_API_KEY`.
- Keep `BASEURL=.` and run from `backend` so downloaded document paths can later be resolved by the API.
- Ensure the configured database is available.

Run one ingestion batch:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=ingestion
```

The ingestion profile runs without a web server. It discovers and downloads the configured Senus documents, extracts and persists supported financial data, calculates derived values, generates analysis, and exits when the batch finishes. Downloaded files are written below `src/main/resources/documents` and are ignored by Git.

The job requires network access to the configured investor-relations host and the OpenAI API. A failed document stops the remaining batch and is recorded in the database.

## Test

Run all backend tests:

```bash
./mvnw test
```

Run one test class when investigating a specific area:

```bash
./mvnw -Dtest=CalculationServiceTest test
```

Tests use mocks for external boundaries and do not require a live OpenAI request.

## Package and Run

Build the executable JAR and run the test suite:

```bash
./mvnw package
```

Run the packaged web application from the `backend` directory:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Run the packaged ingestion job:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=ingestion
```

The same environment variables are required for packaged runs. If the JAR is started from another working directory, set `BASEURL` to the directory against which stored document paths should be resolved.

## Common Problems

### Database connection failure

Check that MySQL is running, the database exists, the credentials are correct, and `DB_URL` uses a JDBC MySQL URL.

### Schema validation failure during ingestion

The ingestion profile does not run Flyway. Start the normal web application against the same database first, then retry ingestion.

### JWT startup or login failure

Use a random `JWT_SECRET_KEY` of at least 32 bytes. Do not reuse the example placeholder.

### Admin bootstrap failure

Provide every `ADMIN_BOOTSTRAP_*` value, use an allowed email domain, and satisfy the 16-character Admin password policy. Disable bootstrap after the first Admin is created.

### Documents are listed without a download action

Run the backend from `backend` with `BASEURL=.`, or set `BASEURL` to the correct absolute runtime directory. The stored document must remain within that base directory and be readable by the application.
