package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.ReportingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Provides persistence operations for reporting periods. */
public interface ReportingPeriodRepository extends JpaRepository<ReportingPeriodEntity, Long> {

    /** Finds the stable reporting period identified by its catalogue code. */
    Optional<ReportingPeriodEntity> findByCode(String code);
}
