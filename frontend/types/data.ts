/** Identifies the currently supported reporting periods. */
export type PeriodCode = "FY2024" | "FY2025" | "HY2025" | "HY2026";

/** Describes reporting-period metadata returned by the backend. */
export type Period = {
  code: string;
  label: string;
  type: "FULL_YEAR" | "HALF_YEAR";
  startDate: string;
  endDate: string;
  isDefault?: boolean;
};

/** Describes AI analysis shared by period and comparison responses. */
export type Analytics = {
  growthAnalytics: string | null;
  profitabilityAnalytics: string | null;
  liquidityAnalytics: string | null;
  capitalAnalytics: string | null;
  totalAnalytics: string | null;
};

/** Describes reported and calculated growth metrics. */
export type Growth = {
  revenue: number | null;
  calculated: {
    revenueGrowth: number | null;
  };
};

/** Describes reported and calculated profitability metrics. */
export type Profitability = {
  grossProfit: number | null;
  grossMargin: number | null;
  operatingLoss: number | null;
  costOfSales: number | null;
  administrativeExpenses: number | null;
  calculated: {
    grossMargin: number | null;
    operatingMargin: number | null;
    costOfSalesRatio: number | null;
    administrativeExpenseRatio: number | null;
  };
};

/** Describes reported and calculated liquidity metrics. */
export type Liquidity = {
  cashBalance: number | null;
  operatingCashFlow: number | null;
  workingCapitalMovement: number | null;
  currentAssets: number | null;
  currentLiabilities: number | null;
  netCurrentPosition: number | null;
  capitalExpenditure: number | null;
  calculated: {
    operatingCashFlowMargin: number | null;
    freeCashFlow: number | null;
    freeCashFlowMargin: number | null;
    currentRatio: number | null;
    cashRatio: number | null;
  };
};

/** Describes reported and calculated capital metrics. */
export type Capital = {
  bankDebt: number | null;
  loanMovement: number | null;
  interestExpense: number | null;
  netAssetPosition: number | null;
  calculated: {
    netCash: number | null;
  };
};

/** Describes the complete financial response for one reporting period. */
export type PeriodData = {
  period: Period;
  growth: Growth;
  profitability: Profitability;
  liquidity: Liquidity;
  capital: Capital;
  analytics: Analytics;
};

/** Describes stored comparison analysis for a supported period pair. */
export type Comparison = {
  basePeriod: Period;
  targetPeriod: Period;
  analytics: Analytics;
};

/** Describes one downloadable source document. */
export type DataDoc = {
  name: string;
  type: string;
  publicationDate: string | null;
  aiSummary: string | null;
  downloadUrl: string | null;
};

/** Wraps the reporting-period list returned by the backend. */
export type PeriodsRes = {
  periods: Period[];
};

/** Wraps the source-document list returned by the backend. */
export type DocumentsRes = {
  documents: DataDoc[];
};
