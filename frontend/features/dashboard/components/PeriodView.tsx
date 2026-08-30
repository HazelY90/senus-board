"use client";

import { useEffect, useState } from "react";
import DataPage from "./DataPage";
import MetricSection from "./MetricSection";
import { getAnalysis, getMetrics } from "../metrics";
import { getCategoryOrder, periodOptions } from "../config";
import { useData } from "../hooks/useData";
import { useAuth } from "@/features/auth/hooks/useAuth";
import type { PeriodCode } from "@/types/data";
import { formatDate } from "@/utils/format";
import colors from "@/public/colors.json";

/** Selects and displays each reporting period within one Dashboard page. */
export default function PeriodView() {
  const { loadPeriod, periodData } = useData();
  const { user } = useAuth();
  const [code, setCode] = useState<PeriodCode>("HY2026");
  const [error, setError] = useState<string | null>(null);
  const data = periodData[code];

  useEffect(() => {
    let isLive = true;

    if (data) return;

    loadPeriod(code)
      .catch((reason) => {
        if (isLive) {
          setError(reason instanceof Error ? reason.message : "Request failed.");
        }
      });

    return () => {
      isLive = false;
    };
  }, [code, data, loadPeriod]);

  const order = getCategoryOrder(user?.role);
  const description = data
    ? `${data.period.type === "FULL_YEAR" ? "Full-year" : "Half-year"} financial results from ${formatDate(data.period.startDate)} to ${formatDate(data.period.endDate)}.`
    : "Select a reporting period to review its complete financial results.";

  /** Changes the active period while preserving cached data for previous tabs. */
  const selectPeriod = (period: PeriodCode) => {
    setError(null);
    setCode(period);
  };

  return (
    <DataPage
      description={description}
      eyebrow="Reporting period"
      title={data?.period.label ?? code}
    >
      <div className="flex flex-wrap gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        {periodOptions.map((period) => (
          <button
            aria-pressed={code === period.code}
            className="cursor-pointer rounded-xl px-4 py-2.5 text-sm font-semibold transition-colors"
            key={period.code}
            style={{
              backgroundColor:
                code === period.code ? colors.light_theme : "#f1f5f4",
              color: code === period.code ? "white" : colors.main_theme,
            }}
            type="button"
            onClick={() => selectPeriod(period.code)}
          >
            {period.label}
          </button>
        ))}
      </div>

      {!data ? (
        <DashboardState
          isError={Boolean(error)}
          message={error ?? `Loading ${code} data...`}
        />
      ) : (
        <>
          <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <p
              className="text-xs font-bold uppercase tracking-[0.16em]"
              style={{ color: colors.light_theme }}
            >
              Overall AI-generated analysis
            </p>
            <p className="mt-3 leading-7 text-slate-700">
              {data.analytics.totalAnalytics ?? "Insufficient data for analysis."}
            </p>
          </section>

          {order.map((category) => (
            <MetricSection
              analytics={getAnalysis(data.analytics, category)}
              category={category}
              key={category}
              metrics={getMetrics(data, category)}
            />
          ))}
        </>
      )}
    </DataPage>
  );
}

/** Displays an inline loading or request-error state below the period tabs. */
function DashboardState({
  isError = false,
  message,
}: {
  isError?: boolean;
  message: string;
}) {
  return (
    <div className="flex min-h-56 items-center justify-center">
      <p
        className={`rounded-2xl border bg-white px-6 py-5 text-sm font-semibold shadow-sm ${
          isError ? "border-red-200 text-red-700" : "border-slate-200"
        }`}
        style={isError ? undefined : { color: colors.main_theme }}
      >
        {message}
      </p>
    </div>
  );
}
