import { categoryMeta, type CategoryId } from "../config";
import type { MetricItem } from "../metrics";
import { formatEur, formatPercent, formatRatio } from "@/utils/format";
import colors from "@/public/colors.json";

type MetricSectionProps = {
  analytics: string | null;
  category: CategoryId;
  metrics: MetricItem[];
};

/** Displays all fixed-schema values and AI analysis for one category. */
export default function MetricSection({
  analytics,
  category,
  metrics,
}: MetricSectionProps) {
  const meta = categoryMeta[category];

  return (
    <section
      className="scroll-mt-28 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7"
      id={category}
    >
      <div className="mb-6 border-b border-slate-100 pb-5">
        <h2
          className="text-2xl font-bold tracking-tight"
          style={{ color: colors.dark_theme }}
        >
          {meta.label}
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">{meta.description}</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {metrics.map((item) => (
          <article
            className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4"
            key={item.label}
          >
            <div className="flex items-start justify-between gap-3">
              <p className="text-sm font-medium text-slate-600">{item.label}</p>
              <span
                className="rounded-full px-2 py-1 text-[0.65rem] font-bold uppercase tracking-wide"
                style={{
                  backgroundColor:
                    item.source === "Calculated" ? "#e7f5f3" : "#eef2f1",
                  color: colors.main_theme,
                }}
              >
                {item.source}
              </span>
            </div>
            <p
              className={`mt-4 text-2xl font-bold tracking-tight ${
                item.value !== null && item.value < 0 ? "text-red-700" : ""
              }`}
              style={
                item.value !== null && item.value < 0
                  ? undefined
                  : { color: colors.dark_theme }
              }
            >
              {formatMetric(item)}
            </p>
          </article>
        ))}
      </div>

      <div className="mt-6 rounded-2xl border border-teal-100 bg-teal-50/60 p-5">
        <p
          className="text-xs font-bold uppercase tracking-[0.16em]"
          style={{ color: colors.light_theme }}
        >
          AI-generated analysis
        </p>
        <p className="mt-3 text-sm leading-7 text-slate-700">
          {analytics ?? "Insufficient data for analysis."}
        </p>
      </div>
    </section>
  );
}

/** Applies the display formatter required by one metric unit. */
function formatMetric(item: MetricItem) {
  if (item.unit === "eur") return formatEur(item.value);
  if (item.unit === "percent") return formatPercent(item.value);
  return formatRatio(item.value);
}
