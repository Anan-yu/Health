package com.rayk.health.healthscan.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.healthscan.entity.HealthScanTaskEntity;
import com.rayk.health.healthscan.mapper.HealthScanTaskMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class HealthScanContextService {
    private final HealthScanTaskMapper taskMapper;

    public HealthScanContextService(HealthScanTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public LatestVitals latest(long tenantId, long patientId) {
        HealthScanTaskEntity task =
                taskMapper.selectOne(
                        new LambdaQueryWrapper<HealthScanTaskEntity>()
                                .eq(HealthScanTaskEntity::getTenantId, tenantId)
                                .eq(HealthScanTaskEntity::getPatientId, patientId)
                                .eq(HealthScanTaskEntity::getStatus, "SUCCEEDED")
                                .eq(HealthScanTaskEntity::getDeleted, 0)
                                .orderByDesc(HealthScanTaskEntity::getCompletedAt)
                                .last("LIMIT 1"));
        if (task == null) {
            return LatestVitals.empty();
        }
        return new LatestVitals(
                task.getHeartRate(),
                task.getHeartRateVariability(),
                task.getOxygenSaturation(),
                task.getRespirationRate(),
                task.getSystolicBloodPressure(),
                task.getDiastolicBloodPressure(),
                task.getStressHrv(),
                task.getQualityScore(),
                task.getCompletedAt());
    }

    public record LatestVitals(
            BigDecimal heartRate,
            BigDecimal heartRateVariability,
            BigDecimal oxygenSaturation,
            BigDecimal respirationRate,
            BigDecimal systolicBloodPressure,
            BigDecimal diastolicBloodPressure,
            BigDecimal stressHrv,
            BigDecimal qualityScore,
            LocalDateTime completedAt) {
        public static LatestVitals empty() {
            return new LatestVitals(null, null, null, null, null, null, null, null, null);
        }
    }
}

