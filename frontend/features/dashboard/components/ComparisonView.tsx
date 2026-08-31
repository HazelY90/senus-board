"use client";

import { useEffect, useState } from "react";
import DataPage from "./DataPage";
import { categoryMeta, getCategoryOrder, type CategoryId } from "../config";
import { getAnalysis, getMetrics, type MetricItem } from "../metrics";
import { useData } from "../hooks/useData";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { formatEur, formatPercent, formatRatio } from "@/utils/format";
import colors from "@/public/colors.json";

const pairs = [
  { base: "FY2024", label: "FY2024 vs FY2025", target: "FY2025" },
  { base: "HY2025", label: "HY2025 vs HY2026", target: "HY2026" },
] as const;

/** Loads and displays one of the two supported reporting-period comparisons. */
export default function ComparisonView() {
  const {
    comparisons,
    loadComparison,
    loadPeriod,
    periodData,
  } = useData();
  const { user } = useAuth();
  const [pairIndex, setPairIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const pair = pairs[pairIndex];
  const baseData = periodData[pair.base];
  const targetData = periodData[pair.target];
  const comparison = comparisons[`${pair.base}:${pair.target}`];

  useEffect(() => {
    let isLive = true;
    if (baseData && targetData && comparison) return;

    Promise.all([
      loadPeriod(pair.base),
      loadPeriod(pair.target),
      loadComparison(pair.base, pair.target),
    ]).catch((reason) => {
      if (isLive) {
        setError(reason instanceof Error ? reason.message : "Request failed.");
      }
    });

    return () => {
      isLive = false;
    };
  }, [
    baseData,
    comparison,
    loadComparison,
    loadPeriod,
    pair.base,
    pair.target,
    targetData,
  ]);

  const selectPair = (index: number) => {
    setError(null);
    setPairIndex(index);
  };

  if (!baseData || !targetData || !comparison) {
    return (
      <main className="mx-auto flex min-h-[60vh] max-w-[1600px] items-center justify-center px-6">
        <p
          className={`rounded-2xl border bg-white px-6 py-5 text-sm font-semibold shadow-sm ${
            error ? "border-red-200 text-red-700" : "border-slate-200"
          }`}
          style={error ? undefined : { color: colors.main_theme }}
        >
          {error ?? `Loading ${pair.label}...`}
        </p>
      </main>
    );
  }

  const order = getCategoryOrder(user?.role);

  return (
    <DataPage
      description="Review equivalent annual or half-year reporting periods without mixing period types."
      eyebrow="Comparable performance"
      title="Period comparison"
    >
      <div className="flex flex-wrap gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        {pairs.map((item, index) => (
          <button
            aria-pressed={pairIndex === index}
            className="cursor-pointer rounded-xl px-4 py-2.5 text-sm font-semibold transition-colors"
            key={item.label}
            style={{
              backgroundColor:
                pairIndex === index ? colors.light_theme : "#f1f5f4",
              color: pairIndex === index ? "white" : colors.main_theme,
            }}
            type="button"
            onClick={() => selectPair(index)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <p
          className="text-xs font-bold uppercase tracking-[0.16em]"
          style={{ color: colors.light_theme }}
        >
          Overall AI-generated comparison
        </p>
        <p className="mt-3 leading-7 text-slate-700">
          {comparison.analytics.totalAnalytics ??
            "Insufficient data for analysis."}
        </p>
      </section>

      {order.map((category) => (
        <ComparisonSection
          analytics={getAnalysis(comparison.analytics, category)}
          baseLabel={comparison.basePeriod.code}
          baseMetrics={getMetrics(baseData, category)}
          category={category}
          key={category}
          targetLabel={comparison.targetPeriod.code}
          targetMetrics={getMetrics(targetData, category)}
        />
      ))}
    </DataPage>
  );
}

type ComparisonSectionProps = {
  analytics: string | null;
  baseLabel: string;
  baseMetrics: MetricItem[];
  category: CategoryId;
  targetLabel: string;
  targetMetrics: MetricItem[];
};

/** Displays aligned metric values for one category across two periods. */
function ComparisonSection({
  analytics,
  baseLabel,
  baseMetrics,
  category,
  targetLabel,
  targetMetrics,
}: ComparisonSectionProps) {
  return (
    <section
      className="scroll-mt-28 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7"
      id={category}
    >
      <h2
        className="text-2xl font-bold tracking-tight"
        style={{ color: colors.dark_theme }}
      >
        {categoryMeta[category].label}
      </h2>
      <p className="mt-2 text-sm leading-6 text-slate-600">
        {categoryMeta[category].description}
      </p>

      <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {baseMetrics.map((baseMetric, index) => (
          <MetricComparisonCard
            base={baseMetric}
            baseLabel={baseLabel}
            key={baseMetric.label}
            target={targetMetrics[index]}
            targetLabel={targetLabel}
          />
        ))}
      </div>

      <div className="mt-6 rounded-2xl border border-teal-100 bg-teal-50/60 p-5">
        <p
          className="text-xs font-bold uppercase tracking-[0.16em]"
          style={{ color: colors.light_theme }}
        >
          AI-generated comparison
        </p>
        <p className="mt-3 text-sm leading-7 text-slate-700">
          {analytics ?? "Insufficient data for analysis."}
        </p>
      </div>
    </section>
  );
}

type MetricComparisonCardProps = {
  base: MetricItem;
  baseLabel: string;
  target: MetricItem;
  targetLabel: string;
};

/** Compares one metric with two signed bars and exact period values. */
function MetricComparisonCard({
  base,
  baseLabel,
  target,
  targetLabel,
}: MetricComparisonCardProps) {
  const max = Math.max(Math.abs(base.value ?? 0), Math.abs(target.value ?? 0));

  return (
    <article className="min-w-0 rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
      <h3 className="break-words text-sm font-bold text-slate-800">
        {base.label}
      </h3>
      <div className="mt-5 grid gap-5">
        <MetricBar
          color={colors.dark_theme}
          item={base}
          label={baseLabel}
          max={max}
        />
        <MetricBar
          color={colors.light_theme}
          item={target}
          label={targetLabel}
          max={max}
        />
      </div>
    </article>
  );
}

type MetricBarProps = {
  color: string;
  item: MetricItem;
  label: string;
  max: number;
};

/** Draws a bar around a shared zero line for positive and negative values. */
function MetricBar({ color, item, label, max }: MetricBarProps) {
  const value = item.value;
  const width = value === null || max === 0 ? 0 : (Math.abs(value) / max) * 50;
  const isNegative = value !== null && value < 0;
  const barColor = isNegative ? "#b91c1c" : color;

  return (
    <div>
      <div className="mb-2 flex min-w-0 items-start justify-between gap-3 text-xs">
        <div className="flex min-w-0 flex-wrap items-center gap-2">
          <span className="font-bold text-slate-700">{label}</span>
          <span className="rounded-full bg-white px-2 py-0.5 font-semibold uppercase tracking-wide text-slate-500">
            {item.source}
          </span>
        </div>
        <span className="shrink-0 font-bold text-slate-900">
          {formatMetric(item)}
        </span>
      </div>
      <div
        aria-label={`${label}: ${formatMetric(item)}`}
        className="relative h-3 overflow-hidden rounded-full bg-slate-200"
        role="img"
      >
        <span className="absolute inset-y-0 left-1/2 z-10 w-px bg-slate-400" />
        {value !== null && (
          <span
            className="absolute inset-y-0 rounded-full"
            style={{
              backgroundColor: barColor,
              ...(isNegative
                ? { right: "50%", width: `${width}%` }
                : { left: "50%", width: value === 0 ? "2px" : `${width}%` }),
            }}
          />
        )}
      </div>
    </div>
  );
}

/** Applies the display formatter required by one comparison metric. */
function formatMetric(item: MetricItem) {
  if (item.unit === "eur") return formatEur(item.value);
  if (item.unit === "percent") return formatPercent(item.value);
  return formatRatio(item.value);
}
