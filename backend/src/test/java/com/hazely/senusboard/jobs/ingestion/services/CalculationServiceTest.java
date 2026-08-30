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
import com.hazely.senusboard.entities.enums.PeriodType;
import com.hazely.senusboard.repositories.CalculatedCapitalRepository;
import com.hazely.senusboard.repositories.CalculatedGrowthRepository;
import com.hazely.senusboard.repositories.CalculatedLiquidityRepository;
import com.hazely.senusboard.repositories.CalculatedProfitabilityRepository;
import com.hazely.senusboard.repositories.CapitalRepository;
import com.hazely.senusboard.repositories.GrowthRepository;
import com.hazely.senusboard.repositories.LiquidityRepository;
import com.hazely.senusboard.repositories.ProfitabilityRepository;
import com.hazely.senusboard.repositories.ReportingPeriodRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalculationServiceTest {

    @Test
    void calculatesPeriodValues() {
        ReportingPeriodRepository periodRepo = mock(ReportingPeriodRepository.class);
        GrowthRepository growthRepo = mock(GrowthRepository.class);
        ProfitabilityRepository profitRepo = mock(ProfitabilityRepository.class);
        LiquidityRepository liquidityRepo = mock(LiquidityRepository.class);
        CapitalRepository capitalRepo = mock(CapitalRepository.class);
        CalculatedGrowthRepository calcGrowthRepo = mock(CalculatedGrowthRepository.class);
        CalculatedProfitabilityRepository calcProfitRepo = mock(CalculatedProfitabilityRepository.class);
        CalculatedLiquidityRepository calcLiquidityRepo = mock(CalculatedLiquidityRepository.class);
        CalculatedCapitalRepository calcCapitalRepo = mock(CalculatedCapitalRepository.class);
        ReportingPeriodEntity prior = period("FY2024", LocalDate.of(2024, 6, 30));
        ReportingPeriodEntity current = period("FY2025", LocalDate.of(2025, 6, 30));
        GrowthEntity priorGrowth = growth(prior, "688317");
        GrowthEntity currentGrowth = growth(current, "836991");
        ProfitabilityEntity profit = profit(current);
        LiquidityEntity liquidity = liquidity(current);
        CapitalEntity capital = capital(current);

        when(periodRepo.findAll()).thenReturn(List.of(prior, current));
        when(growthRepo.findByReportingPeriod(prior)).thenReturn(Optional.of(priorGrowth));
        when(growthRepo.findByReportingPeriod(current)).thenReturn(Optional.of(currentGrowth));
        when(profitRepo.findByReportingPeriod(current)).thenReturn(Optional.of(profit));
        when(liquidityRepo.findByReportingPeriod(current)).thenReturn(Optional.of(liquidity));
        when(capitalRepo.findByReportingPeriod(current)).thenReturn(Optional.of(capital));
        when(calcGrowthRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());
        when(calcProfitRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());
        when(calcLiquidityRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());
        when(calcCapitalRepo.findByReportingPeriod(any())).thenReturn(Optional.empty());

        CalculationService service = new CalculationService(
                periodRepo,
                growthRepo,
                profitRepo,
                liquidityRepo,
                capitalRepo,
                calcGrowthRepo,
                calcProfitRepo,
                calcLiquidityRepo,
                calcCapitalRepo
        );
        service.recalculate();

        CalculatedGrowthEntity calcGrowth = currentGrowth(calcGrowthRepo, current);
        assertThat(calcGrowth.getRevenueGrowth()).isEqualByComparingTo("21.5996");
        CalculatedProfitabilityEntity calcProfit = currentProfit(calcProfitRepo, current);
        assertThat(calcProfit.getCalculatedGrossMargin()).isEqualByComparingTo("77.4740");
        CalculatedLiquidityEntity calcLiquidity = currentLiquidity(calcLiquidityRepo, current);
        assertThat(calcLiquidity.getFreeCashFlow()).isEqualByComparingTo("-379271.0000");
        assertThat(calcLiquidity.getCurrentRatio()).isEqualByComparingTo("1.0791");
        CalculatedCapitalEntity calcCapital = currentCapital(calcCapitalRepo, current);
        assertThat(calcCapital.getNetCash()).isEqualByComparingTo("56480.0000");
    }

    private ReportingPeriodEntity period(String code, LocalDate endDate) {
        ReportingPeriodEntity period = new ReportingPeriodEntity();
        period.setCode(code);
        period.setPeriodType(PeriodType.FULL_YEAR);
        period.setEndDate(endDate);
        return period;
    }

    private GrowthEntity growth(ReportingPeriodEntity period, String revenue) {
        GrowthEntity growth = new GrowthEntity();
        growth.setReportingPeriod(period);
        growth.setRevenue(new BigDecimal(revenue));
        return growth;
    }

    private ProfitabilityEntity profit(ReportingPeriodEntity period) {
        ProfitabilityEntity profit = new ProfitabilityEntity();
        profit.setReportingPeriod(period);
        profit.setGrossProfit(new BigDecimal("648450"));
        profit.setOperatingLoss(new BigDecimal("-633694"));
        profit.setCostOfSales(new BigDecimal("-188541"));
        profit.setAdministrativeExpenses(new BigDecimal("-1286058"));
        return profit;
    }

    private LiquidityEntity liquidity(ReportingPeriodEntity period) {
        LiquidityEntity liquidity = new LiquidityEntity();
        liquidity.setReportingPeriod(period);
        liquidity.setCashBalance(new BigDecimal("140135"));
        liquidity.setOperatingCashFlow(new BigDecimal("-374820"));
        liquidity.setCurrentAssets(new BigDecimal("263138"));
        liquidity.setCurrentLiabilities(new BigDecimal("-243846"));
        liquidity.setCapitalExpenditure(new BigDecimal("-4451"));
        return liquidity;
    }

    private CapitalEntity capital(ReportingPeriodEntity period) {
        CapitalEntity capital = new CapitalEntity();
        capital.setReportingPeriod(period);
        capital.setBankDebt(new BigDecimal("83655"));
        return capital;
    }

    private CalculatedGrowthEntity currentGrowth(
            CalculatedGrowthRepository repo,
            ReportingPeriodEntity period
    ) {
        ArgumentCaptor<CalculatedGrowthEntity> captor = ArgumentCaptor.forClass(CalculatedGrowthEntity.class);
        verify(repo, times(2)).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(item -> item.getReportingPeriod() == period)
                .findFirst()
                .orElseThrow();
    }

    private CalculatedProfitabilityEntity currentProfit(
            CalculatedProfitabilityRepository repo,
            ReportingPeriodEntity period
    ) {
        ArgumentCaptor<CalculatedProfitabilityEntity> captor =
                ArgumentCaptor.forClass(CalculatedProfitabilityEntity.class);
        verify(repo, times(2)).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(item -> item.getReportingPeriod() == period)
                .findFirst()
                .orElseThrow();
    }

    private CalculatedLiquidityEntity currentLiquidity(
            CalculatedLiquidityRepository repo,
            ReportingPeriodEntity period
    ) {
        ArgumentCaptor<CalculatedLiquidityEntity> captor =
                ArgumentCaptor.forClass(CalculatedLiquidityEntity.class);
        verify(repo, times(2)).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(item -> item.getReportingPeriod() == period)
                .findFirst()
                .orElseThrow();
    }

    private CalculatedCapitalEntity currentCapital(
            CalculatedCapitalRepository repo,
            ReportingPeriodEntity period
    ) {
        ArgumentCaptor<CalculatedCapitalEntity> captor =
                ArgumentCaptor.forClass(CalculatedCapitalEntity.class);
        verify(repo, times(2)).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(item -> item.getReportingPeriod() == period)
                .findFirst()
                .orElseThrow();
    }
}
