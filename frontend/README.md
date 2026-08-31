# SenusBoard Frontend

This README covers frontend configuration, startup, production builds, and verification. See the [project README](../README.md) and the linked design records there for product and design documentation.

## Prerequisites

- Node.js 20.9 or later
- npm
- The SenusBoard backend running on a reachable origin

Run every command in this README from the `frontend` directory:

```bash
cd frontend
```

## Install Dependencies

Install the versions recorded in `package-lock.json`:

```bash
npm ci
```

Use `npm ci` for a clean, reproducible installation. Use `npm install` only when intentionally changing dependencies and updating the lockfile.

## Environment Configuration

Create `frontend/.env`. The file is ignored by Git.

```dotenv
API_BASE_URL=http://localhost:8080
```

`API_BASE_URL` is the backend origin used by the Next.js server. It must not include `/api`; the existing rewrite appends the complete `/api/...` request path. A trailing slash is accepted and removed automatically.

This variable is server-only and is not exposed to browser code. The browser always calls the same-origin `/api/v1/...` path, and Next.js forwards that traffic to the configured backend.

Restart the Next.js process after changing `.env`.

## Local Development

Start the backend first, then run the frontend development server:

```bash
npm run dev
```

Open `http://localhost:3000`.

The terminal should report that the application is ready. API-dependent pages require the backend and database to remain available.

Stop the development server with `Ctrl+C`.

## Validate the Frontend

Run ESLint:

```bash
npm run lint
```

Create a production build to run TypeScript, compilation, and route-generation checks:

```bash
npm run build
```

The frontend currently has no separate automated test command. Use both `npm run lint` and `npm run build` before delivery.

## Production Build and Run

Build the application:

```bash
npm run build
```

Start the production server:

```bash
npm run start
```

The production server listens on `http://localhost:3000` by default. Keep `API_BASE_URL` configured for the backend origin that the Next.js server can reach.

If the backend address changes, update `.env` and restart the production process. Rebuild as well when preparing a new deployment artifact.

## Available Commands

| Command | Purpose |
| --- | --- |
| `npm run dev` | Start the Next.js development server |
| `npm run lint` | Run ESLint |
| `npm run build` | Create and validate a production build |
| `npm run start` | Serve the completed production build |

## Common Problems

### API requests fail or return a proxy error

Confirm that the backend is running and that `API_BASE_URL` contains only its origin, for example `http://localhost:8080`. Restart Next.js after changing the value.

### Login succeeds but the session cannot be restored

Access and refresh requests depend on the same-origin `/api` rewrite. Use the frontend URL in the browser rather than calling the backend origin directly, and confirm that the backend is reachable through the configured rewrite.

### Port 3000 is already in use

Start Next.js on another port:

```bash
npm run dev -- --port 3001
```

Open the port reported by Next.js.

### Production start reports that no build exists

Run `npm run build` successfully before `npm run start`.
