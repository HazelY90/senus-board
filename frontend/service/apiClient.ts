import type { ApiOpts, ApiProblem } from "@/types/api";
import type { TokenRes } from "@/types/auth";

const apiPath = "/api/v1";

/** Event dispatched when the refresh-token session can no longer be renewed. */
export const authExpiredEvent = "auth:expired";

/** Represents a non-successful HTTP response returned by the backend API. */
export class ApiError extends Error {
  readonly data: unknown;
  readonly status: number;

  constructor(status: number, data: unknown) {
    super(getMsg(status, data));
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

/** Sends same-origin API requests and manages the access-token lifecycle. */
class ApiClient {
  private refreshReq: Promise<string | null> | null = null;
  private token: string | null = null;

  /** Replaces the in-memory access token after explicit authentication. */
  setToken(token: string) {
    this.token = token;
  }

  /** Removes the current in-memory access token. */
  clearToken() {
    this.token = null;
  }

  /** Reports whether an access token is currently held in memory. */
  hasToken() {
    return this.token !== null;
  }

  /** Sends a request and applies authentication only when explicitly enabled. */
  async request<T>(
    path: string,
    auth: boolean,
    opts: ApiOpts = {},
  ): Promise<T> {
    if (auth && !this.token) {
      const token = await this.refresh();

      if (!token) {
        throw new ApiError(401, { message: "Authentication is required" });
      }
    }

    return this.send<T>(path, auth, opts);
  }

  /** Sends a GET request with an explicit authentication choice. */
  get<T>(path: string, auth: boolean, opts: ApiOpts = {}) {
    return this.request<T>(path, auth, { ...opts, method: "GET" });
  }

  /** Sends a POST request with an explicit authentication choice. */
  post<T>(path: string, body: unknown, auth: boolean, opts: ApiOpts = {}) {
    return this.request<T>(path, auth, { ...opts, body, method: "POST" });
  }

  /** Sends a DELETE request with an explicit authentication choice. */
  delete<T>(path: string, auth: boolean, opts: ApiOpts = {}) {
    return this.request<T>(path, auth, { ...opts, method: "DELETE" });
  }

  /** Opens a streaming response while preserving token refresh behaviour. */
  async stream(path: string, auth: boolean, signal: AbortSignal) {
    if (auth && !this.token) {
      const token = await this.refresh();

      if (!token) {
        throw new ApiError(401, { message: "Authentication is required" });
      }
    }

    return this.openStream(path, auth, signal);
  }

  /** Executes fetch, attaches credentials, and retries once after a 401. */
  private async send<T>(
    path: string,
    auth: boolean,
    opts: ApiOpts,
    retry = true,
  ): Promise<T> {
    const { body, headers: input, responseType = "auto", ...init } = opts;
    const headers = new Headers(input);
    const reqBody = getBody(body, headers);

    headers.set("Accept", "application/json");

    if (auth && this.token) {
      headers.set("Authorization", `Bearer ${this.token}`);
    } else if (!auth) {
      // Guarantee that public requests never carry an authorization header.
      headers.delete("Authorization");
    }

    const res = await fetch(getUrl(path), {
      ...init,
      body: reqBody,
      credentials: "include",
      headers,
    });

    if (res.status === 401 && auth && retry) {
      const token = await this.refresh();

      if (token) {
        return this.send<T>(path, auth, opts, false);
      }
    }

    const data = await readBody(res, res.ok ? responseType : "auto");

    if (!res.ok) {
      throw new ApiError(res.status, data);
    }

    if (isToken(data)) {
      this.token = data.accessToken;
    }

    return data as T;
  }

  /** Opens an SSE-compatible fetch response and retries once after a 401. */
  private async openStream(
    path: string,
    auth: boolean,
    signal: AbortSignal,
    retry = true,
  ): Promise<Response> {
    const headers = new Headers({ Accept: "text/event-stream" });

    if (auth && this.token) {
      headers.set("Authorization", `Bearer ${this.token}`);
    }

    const res = await fetch(getUrl(path), {
      credentials: "include",
      headers,
      method: "GET",
      signal,
    });

    if (res.status === 401 && auth && retry) {
      const token = await this.refresh();
      if (token) return this.openStream(path, auth, signal, false);
    }

    if (!res.ok) {
      throw new ApiError(res.status, await readBody(res));
    }

    return res;
  }

  /** Reuses one refresh request across concurrent authentication failures. */
  private refresh() {
    if (!this.refreshReq) {
      this.refreshReq = this.doRefresh().finally(() => {
        this.refreshReq = null;
      });
    }

    return this.refreshReq;
  }

  /** Exchanges the HttpOnly refresh cookie for a new access token. */
  private async doRefresh(): Promise<string | null> {
    const res = await fetch(`${apiPath}/auth/refresh`, {
      credentials: "include",
      headers: { Accept: "application/json" },
      method: "POST",
    });

    if (!res.ok) {
      this.expire();
      return null;
    }

    const data = await readBody(res);

    if (!isToken(data)) {
      this.expire();
      return null;
    }

    this.token = data.accessToken;
    return this.token;
  }

  /** Clears authentication and informs the authentication context. */
  private expire() {
    this.token = null;

    if (typeof window !== "undefined") {
      window.dispatchEvent(new Event(authExpiredEvent));
    }
  }
}

/** Converts a relative service path into the versioned same-origin API path. */
function getUrl(path: string) {
  if (path === apiPath || path.startsWith(`${apiPath}/`)) {
    return path;
  }

  return `${apiPath}/${path.replace(/^\/+/, "")}`;
}

/** Serializes JSON values while preserving native fetch body values. */
function getBody(body: unknown, headers: Headers): BodyInit | undefined {
  if (body === undefined) return undefined;
  if (isBody(body)) return body;

  headers.set("Content-Type", "application/json");
  return JSON.stringify(body);
}

/** Identifies body values that fetch can send without JSON serialization. */
function isBody(body: unknown): body is BodyInit {
  return (
    typeof body === "string" ||
    body instanceof Blob ||
    body instanceof FormData ||
    body instanceof URLSearchParams ||
    body instanceof ArrayBuffer ||
    ArrayBuffer.isView(body) ||
    body instanceof ReadableStream
  );
}

/** Validates the shape of an access-token response at runtime. */
function isToken(data: unknown): data is TokenRes {
  return (
    typeof data === "object" &&
    data !== null &&
    "accessToken" in data &&
    typeof data.accessToken === "string"
  );
}

/** Reads JSON, text, empty, and no-content responses consistently. */
async function readBody(
  res: Response,
  responseType: "auto" | "blob" = "auto",
): Promise<unknown> {
  if (res.status === 204) return undefined;
  if (responseType === "blob") return res.blob();

  const text = await res.text();
  if (!text) return undefined;

  if (res.headers.get("content-type")?.includes("application/json")) {
    try {
      return JSON.parse(text) as unknown;
    } catch {
      return text;
    }
  }

  return text;
}

/** Selects the most useful error message from a backend response. */
function getMsg(status: number, data: unknown) {
  if (typeof data === "string" && data) return data;

  if (typeof data === "object" && data !== null) {
    const problem = data as ApiProblem;
    if (problem.detail) return problem.detail;
    if (problem.message) return problem.message;
    if (problem.title) return problem.title;

    // Surface backend field-validation messages when no standard error exists.
    const fields = Object.values(problem).filter(
      (value): value is string => typeof value === "string" && value.length > 0,
    );
    if (fields.length > 0) return fields.join(" ");
  }

  return `Request failed with status ${status}`;
}

/** Shared API client used by all frontend feature modules. */
export const apiClient = new ApiClient();
