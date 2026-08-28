package com.hazely.senusboard.jobs.ingestion.dtos;

import java.math.BigDecimal;
import java.util.List;

/** Represents provider-neutral structured output for one source document. */
public record AiExtractionResult(String publicationDate, String aiSummary, List<PeriodData> periods) {

    /** Contains one reporting period and its stable category values. */
    public record PeriodData(
            String startDate,
            String endDate,
            Growth growth,
            Profitability profitability,
            Liquidity liquidity,
            Capital capital
    ) {
    }

    /** Contains stable growth values. */
    public record Growth(BigDecimal revenue) {
    }

    /** Contains stable profitability values. */
    public record Profitability(
            BigDecimal grossProfit,
            BigDecimal grossMargin,
            BigDecimal operatingLoss,
            BigDecimal costOfSales,
            BigDecimal administrativeExpenses
    ) {
    }

    /** Contains stable liquidity values. */
    public record Liquidity(
            BigDecimal cashBalance,
            BigDecimal operatingCashFlow,
            BigDecimal workingCapitalMovement,
            BigDecimal currentAssets,
            BigDecimal currentLiabilities,
            BigDecimal netCurrentPosition,
            BigDecimal capitalExpenditure
    ) {
    }

    /** Contains stable capital values. */
    public record Capital(
            BigDecimal bankDebt,
            BigDecimal loanMovement,
            BigDecimal interestExpense,
            BigDecimal netAssetPosition
    ) {
    }
}
