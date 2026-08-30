/** Extends fetch options with a body that may be serialized as JSON. */
export type ApiOpts = Omit<RequestInit, "body" | "headers"> & {
  body?: unknown;
  headers?: HeadersInit;
  responseType?: "auto" | "blob";
};

/** Describes the common error fields returned by the backend API. */
export type ApiProblem = {
  detail?: string;
  message?: string;
  title?: string;
  [key: string]: unknown;
};
