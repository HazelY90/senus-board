package com.hazely.senusboard.jobs.ingestion.dtos;

import java.math.BigDecimal;
import java.util.List;

/** Contains the complete stored dataset supplied for AI analytics. */
public record AnalyticsDataset(List<PeriodData> periods) {

    /** Contains reported and calculated values for one period. */
    public record PeriodData(
            String code,
            String label,
            String periodType,
            String startDate,
            String endDate,
            Growth growth,
            Profitability profitability,
            Liquidity liquidity,
            Capital capital
    ) {
    }

    /** Separates reported and calculated growth values. */
    public record Growth(AiExtractionResult.Growth reported, CalculatedGrowth calculated) {
    }

    /** Contains calculated growth values. */
    public record CalculatedGrowth(BigDecimal revenueGrowth) {
    }

    /** Separates reported and calculated profitability values. */
    public record Profitability(
            AiExtractionResult.Profitability reported,
            CalculatedProfitability calculated
    ) {
    }

    /** Contains calculated profitability values. */
    public record CalculatedProfitability(
            BigDecimal grossMargin,
            BigDecimal operatingMargin,
            BigDecimal costOfSalesRatio,
            BigDecimal administrativeExpenseRatio
    ) {
    }

    /** Separates reported and calculated liquidity values. */
    public record Liquidity(
            AiExtractionResult.Liquidity reported,
            CalculatedLiquidity calculated
    ) {
    }

    /** Contains calculated liquidity values. */
    public record CalculatedLiquidity(
            BigDecimal operatingCashFlowMargin,
            BigDecimal freeCashFlow,
            BigDecimal freeCashFlowMargin,
            BigDecimal currentRatio,
            BigDecimal cashRatio
    ) {
    }

    /** Separates reported and calculated capital values. */
    public record Capital(AiExtractionResult.Capital reported, CalculatedCapital calculated) {
    }

    /** Contains calculated capital values. */
    public record CalculatedCapital(BigDecimal netCash) {
    }
}
