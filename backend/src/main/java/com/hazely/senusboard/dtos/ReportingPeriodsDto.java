package com.hazely.senusboard.dtos;

import com.hazely.senusboard.entities.enums.PeriodType;

import java.time.LocalDate;
import java.util.List;

/** Returns the available reporting periods. */
public record ReportingPeriodsDto(List<PeriodDto> periods) {

    /** Describes one selectable reporting period. */
    public record PeriodDto(
            String code,
            String label,
            PeriodType type,
            LocalDate startDate,
            LocalDate endDate,
            boolean isDefault
    ) {
    }
}
