package com.hazely.senusboard.dtos;

import com.hazely.senusboard.entities.enums.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Returns the complete reported, calculated, and analytical dataset for one period. */
public record DataDto(
        PeriodDto period,
        GrowthDto growth,
        ProfitabilityDto profitability,
        LiquidityDto liquidity,
        CapitalDto capital,
        AnalyticsDto analytics
) {

    /** Identifies the reporting period represented by the response. */
    public record PeriodDto(
            String code,
            String label,
            PeriodType type,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    /** Contains reported and calculated growth values. */
    public record GrowthDto(
            BigDecimal revenue,
            GrowthCalcDto calculated
    ) {
    }

    /** Contains calculated growth values. */
    public record GrowthCalcDto(BigDecimal revenueGrowth) {
    }

    /** Contains reported and calculated profitability values. */
    public record ProfitabilityDto(
            BigDecimal grossProfit,
            BigDecimal grossMargin,
            BigDecimal operatingLoss,
            BigDecimal costOfSales,
            BigDecimal administrativeExpenses,
            ProfitabilityCalcDto calculated
    ) {
    }

    /** Contains calculated profitability values. */
    public record ProfitabilityCalcDto(
            BigDecimal grossMargin,
            BigDecimal operatingMargin,
            BigDecimal costOfSalesRatio,
            BigDecimal administrativeExpenseRatio
    ) {
    }

    /** Contains reported and calculated liquidity values. */
    public record LiquidityDto(
            BigDecimal cashBalance,
            BigDecimal operatingCashFlow,
            BigDecimal workingCapitalMovement,
            BigDecimal currentAssets,
            BigDecimal currentLiabilities,
            BigDecimal netCurrentPosition,
            BigDecimal capitalExpenditure,
            LiquidityCalcDto calculated
    ) {
    }

    /** Contains calculated liquidity values. */
    public record LiquidityCalcDto(
            BigDecimal operatingCashFlowMargin,
            BigDecimal freeCashFlow,
            BigDecimal freeCashFlowMargin,
            BigDecimal currentRatio,
            BigDecimal cashRatio
    ) {
    }

    /** Contains reported and calculated capital values. */
    public record CapitalDto(
            BigDecimal bankDebt,
            BigDecimal loanMovement,
            BigDecimal interestExpense,
            BigDecimal netAssetPosition,
            CapitalCalcDto calculated
    ) {
    }

    /** Contains calculated capital values. */
    public record CapitalCalcDto(BigDecimal netCash) {
    }

    /** Contains AI analysis for the reporting period. */
    public record AnalyticsDto(
            String growthAnalytics,
            String profitabilityAnalytics,
            String liquidityAnalytics,
            String capitalAnalytics,
            String totalAnalytics
    ) {
    }
}
