import type { UserRole } from "@/types/auth";
import type { PeriodCode } from "@/types/data";

export type CategoryId = "growth" | "profitability" | "liquidity" | "capital";

/** Defines the reporting-period tabs shown within the Dashboard page. */
export const periodOptions: { code: PeriodCode; label: string }[] = [
  { code: "FY2024", label: "FY2024" },
  { code: "FY2025", label: "FY2025" },
  { code: "HY2025", label: "HY2025" },
  { code: "HY2026", label: "HY2026" },
];

/** Supplies display labels and descriptions for each financial category. */
export const categoryMeta: Record<
  CategoryId,
  { label: string; description: string }
> = {
  growth: {
    label: "Growth",
    description: "Revenue performance and comparable-period growth.",
  },
  profitability: {
    label: "Profitability",
    description: "Margins, operating result, and disclosed operating costs.",
  },
  liquidity: {
    label: "Liquidity",
    description: "Cash generation, working capital, and short-term coverage.",
  },
  capital: {
    label: "Capital",
    description: "Debt, financing movements, and balance-sheet position.",
  },
};

const defaultOrder: CategoryId[] = [
  "growth",
  "profitability",
  "liquidity",
  "capital",
];

/** Returns category priority without hiding any available financial data. */
export function getCategoryOrder(role?: UserRole): CategoryId[] {
  if (role === "BOARD" || role === "EQUITY_INVESTOR") {
    return role === "BOARD"
      ? ["capital", "liquidity", "growth", "profitability"]
      : ["capital", "growth", "profitability", "liquidity"];
  }

  if (role === "CREDIT_PROVIDER") {
    return ["liquidity", "capital", "profitability", "growth"];
  }

  return defaultOrder;
}
