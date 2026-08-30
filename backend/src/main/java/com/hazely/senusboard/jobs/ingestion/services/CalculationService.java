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
import com.hazely.senusboard.repositories.CalculatedCapitalRepository;
import com.hazely.senusboard.repositories.CalculatedGrowthRepository;
import com.hazely.senusboard.repositories.CalculatedLiquidityRepository;
import com.hazely.senusboard.repositories.CalculatedProfitabilityRepository;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Recalculates deterministic values from stored reporting data. */
public class CalculationService {

    private static final int SCALE = 4;
    private static final int CALC_SCALE = 12;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportingPeriodRepository periodRepo;
    private final GrowthRepository growthRepo;
    private final ProfitabilityRepository profitRepo;
    private final LiquidityRepository liquidityRepo;
    private final CapitalRepository capitalRepo;
    private final CalculatedGrowthRepository calcGrowthRepo;
    private final CalculatedProfitabilityRepository calcProfitRepo;
    private final CalculatedLiquidityRepository calcLiquidityRepo;
    private final CalculatedCapitalRepository calcCapitalRepo;

    public CalculationService(
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

    /** Recalculates every period so dependent growth values remain current. */
    public void recalculate() {
        List<ReportingPeriodEntity> periods = periodRepo.findAll();
        for (ReportingPeriodEntity period : periods) {
            GrowthEntity growth = growthRepo.findByReportingPeriod(period).orElse(null);
            ProfitabilityEntity profit = profitRepo.findByReportingPeriod(period).orElse(null);
            LiquidityEntity liquidity = liquidityRepo.findByReportingPeriod(period).orElse(null);
            CapitalEntity capital = capitalRepo.findByReportingPeriod(period).orElse(null);
            saveGrowth(period, growth, periods);
            saveProfit(period, growth, profit);
            saveLiquidity(period, growth, liquidity);
            saveCapital(period, liquidity, capital);
        }
    }

    private void saveGrowth(
            ReportingPeriodEntity period,
            GrowthEntity growth,
            List<ReportingPeriodEntity> periods
    ) {
        CalculatedGrowthEntity row = calcGrowthRepo.findByReportingPeriod(period)
                .orElseGet(CalculatedGrowthEntity::new);
        row.setReportingPeriod(period);
        ReportingPeriodEntity prior = findPrior(period, periods);
        BigDecimal current = growth == null ? null : growth.getRevenue();
        BigDecimal previous = prior == null
                ? null
                : growthRepo.findByReportingPeriod(prior).map(GrowthEntity::getRevenue).orElse(null);
        row.setRevenueGrowth(growth(current, previous));
        calcGrowthRepo.save(row);
    }

    private void saveProfit(
            ReportingPeriodEntity period,
            GrowthEntity growth,
            ProfitabilityEntity profit
    ) {
        CalculatedProfitabilityEntity row = calcProfitRepo.findByReportingPeriod(period)
                .orElseGet(CalculatedProfitabilityEntity::new);
        row.setReportingPeriod(period);
        BigDecimal revenue = growth == null ? null : growth.getRevenue();
        row.setCalculatedGrossMargin(pct(value(profit, ProfitabilityEntity::getGrossProfit), revenue));
        row.setOperatingMargin(pct(value(profit, ProfitabilityEntity::getOperatingLoss), revenue));
        row.setCostOfSalesRatio(pct(abs(value(profit, ProfitabilityEntity::getCostOfSales)), revenue));
        row.setAdministrativeExpenseRatio(pct(
                abs(value(profit, ProfitabilityEntity::getAdministrativeExpenses)),
                revenue
        ));
        calcProfitRepo.save(row);
    }

    private void saveLiquidity(
            ReportingPeriodEntity period,
            GrowthEntity growth,
            LiquidityEntity liquidity
    ) {
        CalculatedLiquidityEntity row = calcLiquidityRepo.findByReportingPeriod(period)
                .orElseGet(CalculatedLiquidityEntity::new);
        row.setReportingPeriod(period);
        BigDecimal revenue = growth == null ? null : growth.getRevenue();
        BigDecimal cashFlow = value(liquidity, LiquidityEntity::getOperatingCashFlow);
        BigDecimal capex = value(liquidity, LiquidityEntity::getCapitalExpenditure);
        BigDecimal freeCashFlow = add(cashFlow, capex);
        BigDecimal liabilities = abs(value(liquidity, LiquidityEntity::getCurrentLiabilities));
        row.setOperatingCashFlowMargin(pct(cashFlow, revenue));
        row.setFreeCashFlow(freeCashFlow);
        row.setFreeCashFlowMargin(pct(freeCashFlow, revenue));
        row.setCurrentRatio(ratio(value(liquidity, LiquidityEntity::getCurrentAssets), liabilities));
        row.setCashRatio(ratio(value(liquidity, LiquidityEntity::getCashBalance), liabilities));
        calcLiquidityRepo.save(row);
    }

    private void saveCapital(
            ReportingPeriodEntity period,
            LiquidityEntity liquidity,
            CapitalEntity capital
    ) {
        CalculatedCapitalEntity row = calcCapitalRepo.findByReportingPeriod(period)
                .orElseGet(CalculatedCapitalEntity::new);
        row.setReportingPeriod(period);
        row.setNetCash(subtract(
                value(liquidity, LiquidityEntity::getCashBalance),
                value(capital, CapitalEntity::getBankDebt)
        ));
        calcCapitalRepo.save(row);
    }

    private ReportingPeriodEntity findPrior(
            ReportingPeriodEntity period,
            List<ReportingPeriodEntity> periods
    ) {
        return periods.stream()
                .filter(item -> item.getPeriodType() == period.getPeriodType())
                .filter(item -> item.getEndDate().equals(period.getEndDate().minusYears(1)))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal growth(BigDecimal current, BigDecimal previous) {
        if (current == null || isZero(previous)) {
            return null;
        }
        return current.divide(previous, CALC_SCALE, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal pct(BigDecimal value, BigDecimal base) {
        if (value == null || isZero(base)) {
            return null;
        }
        return value.divide(base, CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal value, BigDecimal base) {
        if (value == null || isZero(base)) {
            return null;
        }
        return value.divide(base, CALC_SCALE, RoundingMode.HALF_UP)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal add(BigDecimal left, BigDecimal right) {
        return left == null || right == null
                ? null
                : left.add(right).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left == null || right == null
                ? null
                : left.subtract(right).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal abs(BigDecimal value) {
        return value == null ? null : value.abs();
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private <T> BigDecimal value(T row, ValueReader<T> reader) {
        return row == null ? null : reader.read(row);
    }

    @FunctionalInterface
    private interface ValueReader<T> {

        BigDecimal read(T row);
    }
}
