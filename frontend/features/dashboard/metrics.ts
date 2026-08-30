import type { CategoryId } from "./config";
import type { Analytics, PeriodData } from "@/types/data";

export type MetricUnit = "eur" | "percent" | "ratio";

export type MetricItem = {
  label: string;
  source: "Reported" | "Calculated";
  unit: MetricUnit;
  value: number | null;
};

/** Returns the fixed-schema metric list for one Dashboard category. */
export function getMetrics(data: PeriodData, category: CategoryId): MetricItem[] {
  if (category === "growth") {
    return [
      metric("Revenue", data.growth.revenue, "eur"),
      metric(
        "Revenue growth",
        data.growth.calculated.revenueGrowth,
        "percent",
        "Calculated",
      ),
    ];
  }

  if (category === "profitability") {
    const profit = data.profitability;
    const isGrossReported = profit.grossMargin !== null;

    return [
      metric("Gross profit", profit.grossProfit, "eur"),
      metric(
        "Gross margin",
        profit.grossMargin ?? profit.calculated.grossMargin,
        "percent",
        isGrossReported ? "Reported" : "Calculated",
      ),
      metric("Operating loss", profit.operatingLoss, "eur"),
      metric("Cost of sales", profit.costOfSales, "eur"),
      metric(
        "Administrative expenses",
        profit.administrativeExpenses,
        "eur",
      ),
      metric(
        "Operating margin",
        profit.calculated.operatingMargin,
        "percent",
        "Calculated",
      ),
      metric(
        "Cost of sales ratio",
        profit.calculated.costOfSalesRatio,
        "percent",
        "Calculated",
      ),
      metric(
        "Administrative expense ratio",
        profit.calculated.administrativeExpenseRatio,
        "percent",
        "Calculated",
      ),
    ];
  }

  if (category === "liquidity") {
    return [
      metric("Cash balance", data.liquidity.cashBalance, "eur"),
      metric("Operating cash flow", data.liquidity.operatingCashFlow, "eur"),
      metric(
        "Working capital movement",
        data.liquidity.workingCapitalMovement,
        "eur",
      ),
      metric("Current assets", data.liquidity.currentAssets, "eur"),
      metric("Current liabilities", data.liquidity.currentLiabilities, "eur"),
      metric("Net current position", data.liquidity.netCurrentPosition, "eur"),
      metric("Capital expenditure", data.liquidity.capitalExpenditure, "eur"),
      metric(
        "Operating cash flow margin",
        data.liquidity.calculated.operatingCashFlowMargin,
        "percent",
        "Calculated",
      ),
      metric(
        "Free cash flow",
        data.liquidity.calculated.freeCashFlow,
        "eur",
        "Calculated",
      ),
      metric(
        "Free cash flow margin",
        data.liquidity.calculated.freeCashFlowMargin,
        "percent",
        "Calculated",
      ),
      metric(
        "Current ratio",
        data.liquidity.calculated.currentRatio,
        "ratio",
        "Calculated",
      ),
      metric(
        "Cash ratio",
        data.liquidity.calculated.cashRatio,
        "ratio",
        "Calculated",
      ),
    ];
  }

  return [
    metric("Bank debt", data.capital.bankDebt, "eur"),
    metric("Loan movement", data.capital.loanMovement, "eur"),
    metric("Interest expense", data.capital.interestExpense, "eur"),
    metric("Net asset position", data.capital.netAssetPosition, "eur"),
    metric(
      "Net cash",
      data.capital.calculated.netCash,
      "eur",
      "Calculated",
    ),
  ];
}

/** Returns the category-specific AI analysis field. */
export function getAnalysis(analytics: Analytics, category: CategoryId) {
  if (category === "growth") return analytics.growthAnalytics;
  if (category === "profitability") return analytics.profitabilityAnalytics;
  if (category === "liquidity") return analytics.liquidityAnalytics;
  return analytics.capitalAnalytics;
}

/** Creates one consistently classified fixed-schema metric item. */
function metric(
  label: string,
  value: number | null,
  unit: MetricUnit,
  source: MetricItem["source"] = "Reported",
): MetricItem {
  return { label, source, unit, value };
}
