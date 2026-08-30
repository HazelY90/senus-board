import { apiClient } from "./apiClient";

export type RevokedStatus = "DISABLED" | "REJECTED";

/** Reads authenticated account-status events from the backend SSE stream. */
class AuthEventService {
  async listen(
    onRevoked: (status: RevokedStatus) => void,
    signal: AbortSignal,
  ) {
    const res = await apiClient.stream("/auth/events", true, signal);
    if (!res.body) throw new Error("Account event stream is unavailable.");

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const parsed = readBlocks(buffer);
      buffer = parsed.rest;

      for (const block of parsed.blocks) {
        const status = getStatus(block);
        if (status) onRevoked(status);
      }
    }
  }
}

/** Extracts complete SSE blocks while retaining an incomplete trailing block. */
function readBlocks(value: string) {
  const blocks: string[] = [];
  let rest = value;
  let match = rest.match(/\r?\n\r?\n/);

  while (match?.index !== undefined) {
    blocks.push(rest.slice(0, match.index));
    rest = rest.slice(match.index + match[0].length);
    match = rest.match(/\r?\n\r?\n/);
  }

  return { blocks, rest };
}

/** Returns a supported status from an account-access-revoked event. */
function getStatus(block: string): RevokedStatus | null {
  const lines = block.replaceAll("\r\n", "\n").split("\n");
  const event = lines.find((line) => line.startsWith("event:"))?.slice(6).trim();
  if (event !== "account-access-revoked") return null;

  const text = lines
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice(5).trimStart())
    .join("\n");

  try {
    const data = JSON.parse(text) as { status?: unknown };
    return data.status === "DISABLED" || data.status === "REJECTED"
      ? data.status
      : null;
  } catch {
    return null;
  }
}

/** Shared authenticated account-event service. */
export const authEventService = new AuthEventService();
