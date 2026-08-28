package com.hazely.senusboard.jobs.ingestion.services;

import com.hazely.senusboard.entities.CalculatedCapitalEntity;
import com.hazely.senusboard.entities.CalculatedGrowthEntity;
import com.hazely.senusboard.entities.CalculatedLiquidityEntity;
import com.hazely.senusboard.entities.CalculatedProfitabilityEntity;
import com.hazely.senusboard.entities.CapitalEntity;
import com.hazely.senusboard.entities.GrowthEntity;
import com.hazely.senusboard.entities.LiquidityEntity;
import com.hazely.senusboard.entities.ProfitabilityEntity;
import com.hazely.senusboard.entities.ReportingPeriodEntity;
import com.hazely.senusboard.jobs.ingestion.AiClient;
import com.hazely.senusboard.jobs.ingestion.dtos.AiExtractionResult;
import com.hazely.senusboard.jobs.ingestion.dtos.AnalyticsDataset;
import com.hazely.senusboard.repositories.CalculatedCapitalRepository;
import com.hazely.senusboard.repositories.CalculatedGrowthRepository;
import com.hazely.senusboard.repositories.CalculatedLiquidityRepository;
import com.hazely.senusboard.repositories.CalculatedProfitabilityRepository;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/** Builds the complete dataset and coordinates AI analytics generation. */
public class AnalyticsService {

    private final AiClient aiClient;
    private final AnalyticsPersistenceService persistenceService;
    private final ReportingPeriodRepository periodRepo;
    private final GrowthRepository growthRepo;
    private final ProfitabilityRepository profitRepo;
    private final LiquidityRepository liquidityRepo;
    private final CapitalRepository capitalRepo;
    private final CalculatedGrowthRepository calcGrowthRepo;
    private final CalculatedProfitabilityRepository calcProfitRepo;
    private final CalculatedLiquidityRepository calcLiquidityRepo;
    private final CalculatedCapitalRepository calcCapitalRepo;

    public AnalyticsService(
            AiClient aiClient,
            AnalyticsPersistenceService persistenceService,
            ReportingPeriodRepository periodRepo,
            GrowthRepository growthRepo,
            ProfitabilityRepository profitRepo,
            LiquidityRepository liquidityRepo,
            CapitalRepository capitalRepo,
            CalculatedGrowthRepository calcGrowthRepo,
            CalculatedProfitabilityRepository calcProfitRepo,
            CalculatedLiquidityRepository calcLiquidityRepo,
            CalculatedCapitalRepository calcCapitalRepo
    ) {
        this.aiClient = aiClient;
        this.persistenceService = persistenceService;
        this.periodRepo = periodRepo;
        this.growthRepo = growthRepo;
        this.profitRepo = profitRepo;
        this.liquidityRepo = liquidityRepo;
        this.capitalRepo = capitalRepo;
        this.calcGrowthRepo = calcGrowthRepo;
        this.calcProfitRepo = calcProfitRepo;
        this.calcLiquidityRepo = calcLiquidityRepo;
        this.calcCapitalRepo = calcCapitalRepo;
    }

    /** Requests and stores analytics for the complete reporting dataset. */
    public void analyze() throws IOException, InterruptedException {
        AnalyticsDataset data = load();
        if (data.periods().isEmpty()) {
            return;
        }
        persistenceService.save(aiClient.analyze(data));
    }

    private AnalyticsDataset load() {
        List<AnalyticsDataset.PeriodData> periods = periodRepo.findAll().stream()
                .sorted(Comparator.comparing(ReportingPeriodEntity::getEndDate))
                .map(this::periodData)
                .toList();
        return new AnalyticsDataset(periods);
    }

    private AnalyticsDataset.PeriodData periodData(ReportingPeriodEntity period) {
        GrowthEntity growth = growthRepo.findByReportingPeriod(period).orElse(null);
        ProfitabilityEntity profit = profitRepo.findByReportingPeriod(period).orElse(null);
        LiquidityEntity liquidity = liquidityRepo.findByReportingPeriod(period).orElse(null);
        CapitalEntity capital = capitalRepo.findByReportingPeriod(period).orElse(null);
        CalculatedGrowthEntity calcGrowth = calcGrowthRepo.findByReportingPeriod(period).orElse(null);
        CalculatedProfitabilityEntity calcProfit = calcProfitRepo.findByReportingPeriod(period).orElse(null);
        CalculatedLiquidityEntity calcLiquidity = calcLiquidityRepo.findByReportingPeriod(period).orElse(null);
        CalculatedCapitalEntity calcCapital = calcCapitalRepo.findByReportingPeriod(period).orElse(null);
        return new AnalyticsDataset.PeriodData(
                period.getCode(),
                period.getLabel(),
                period.getPeriodType().name(),
                period.getStartDate().toString(),
                period.getEndDate().toString(),
                growthData(growth, calcGrowth),
                profitData(profit, calcProfit),
                liquidityData(liquidity, calcLiquidity),
                capitalData(capital, calcCapital)
        );
    }

    private AnalyticsDataset.Growth growthData(
            GrowthEntity reported,
            CalculatedGrowthEntity calculated
    ) {
        AiExtractionResult.Growth values = reported == null
                ? null
                : new AiExtractionResult.Growth(reported.getRevenue());
        AnalyticsDataset.CalculatedGrowth calc = calculated == null
                ? null
                : new AnalyticsDataset.CalculatedGrowth(calculated.getRevenueGrowth());
        return new AnalyticsDataset.Growth(values, calc);
    }

    private AnalyticsDataset.Profitability profitData(
            ProfitabilityEntity reported,
            CalculatedProfitabilityEntity calculated
    ) {
        AiExtractionResult.Profitability values = reported == null
                ? null
                : new AiExtractionResult.Profitability(
                reported.getGrossProfit(),
                reported.getGrossMargin(),
                reported.getOperatingLoss(),
                reported.getCostOfSales(),
                reported.getAdministrativeExpenses()
        );
        AnalyticsDataset.CalculatedProfitability calc = calculated == null
                ? null
                : new AnalyticsDataset.CalculatedProfitability(
                calculated.getCalculatedGrossMargin(),
                calculated.getOperatingMargin(),
                calculated.getCostOfSalesRatio(),
                calculated.getAdministrativeExpenseRatio()
        );
        return new AnalyticsDataset.Profitability(values, calc);
    }

    private AnalyticsDataset.Liquidity liquidityData(
            LiquidityEntity reported,
            CalculatedLiquidityEntity calculated
    ) {
        AiExtractionResult.Liquidity values = reported == null
                ? null
                : new AiExtractionResult.Liquidity(
                reported.getCashBalance(),
                reported.getOperatingCashFlow(),
                reported.getWorkingCapitalMovement(),
                reported.getCurrentAssets(),
                reported.getCurrentLiabilities(),
                reported.getNetCurrentPosition(),
                reported.getCapitalExpenditure()
        );
        AnalyticsDataset.CalculatedLiquidity calc = calculated == null
                ? null
                : new AnalyticsDataset.CalculatedLiquidity(
                calculated.getOperatingCashFlowMargin(),
                calculated.getFreeCashFlow(),
                calculated.getFreeCashFlowMargin(),
                calculated.getCurrentRatio(),
                calculated.getCashRatio()
        );
        return new AnalyticsDataset.Liquidity(values, calc);
    }

    private AnalyticsDataset.Capital capitalData(
            CapitalEntity reported,
            CalculatedCapitalEntity calculated
    ) {
        AiExtractionResult.Capital values = reported == null
                ? null
                : new AiExtractionResult.Capital(
                reported.getBankDebt(),
                reported.getLoanMovement(),
                reported.getInterestExpense(),
                reported.getNetAssetPosition()
        );
        AnalyticsDataset.CalculatedCapital calc = calculated == null
                ? null
                : new AnalyticsDataset.CalculatedCapital(calculated.getNetCash());
        return new AnalyticsDataset.Capital(values, calc);
    }
}
