package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.StrategicTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides persistence operations for strategic targets. */
public interface StrategicTargetRepository extends JpaRepository<StrategicTargetEntity, Long> {
}
