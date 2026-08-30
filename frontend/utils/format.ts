/** Formats an optional EUR amount while preserving its accounting sign. */
export function formatEur(value: number | null) {
  if (value === null) return "Unavailable";

  return new Intl.NumberFormat("en-IE", {
    currency: "EUR",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

/** Formats an optional percentage value using the stored display scale. */
export function formatPercent(value: number | null) {
  if (value === null) return "Unavailable";
  return `${value.toFixed(1)}%`;
}

/** Formats an optional financial ratio with an explicit ratio suffix. */
export function formatRatio(value: number | null) {
  if (value === null) return "Unavailable";
  return `${value.toFixed(2)}x`;
}

/** Formats an ISO date for concise user-facing display. */
export function formatDate(value: string | null) {
  if (!value) return "Unavailable";

  return new Intl.DateTimeFormat("en-IE", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}
