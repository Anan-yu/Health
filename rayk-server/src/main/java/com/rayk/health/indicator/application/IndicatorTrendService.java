package com.rayk.health.indicator.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.indicator.mapper.IndicatorValueMapper;
import com.rayk.health.indicator.vo.TrendPointVo;
import com.rayk.health.indicator.vo.TrendSummaryVo;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.membership.application.MembershipEntitlementService;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class IndicatorTrendService {
    private final IndicatorValueMapper indicatorValueMapper;
    private final LabReportMapper labReportMapper;
    private final DataScopeService dataScopeService;
    private final MembershipEntitlementService membershipEntitlementService;

    public IndicatorTrendService(
            IndicatorValueMapper indicatorValueMapper,
            LabReportMapper labReportMapper,
            DataScopeService dataScopeService,
            MembershipEntitlementService membershipEntitlementService) {
        this.indicatorValueMapper = indicatorValueMapper;
        this.labReportMapper = labReportMapper;
        this.dataScopeService = dataScopeService;
        this.membershipEntitlementService = membershipEntitlementService;
    }

    public List<TrendPointVo> getTrend(long patientId, String indicatorCode, int months) {
        dataScopeService.requirePatient(patientId);
        LocalDate since = trendSince(months);

        LambdaQueryWrapper<IndicatorValueEntity> query = new LambdaQueryWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getPatientId, patientId)
                        .eq(IndicatorValueEntity::getIndicatorCode, indicatorCode)
                        .eq(IndicatorValueEntity::getManuallyConfirmed, 1)
                        .eq(IndicatorValueEntity::getDeleted, 0)
                        .orderByAsc(IndicatorValueEntity::getCreatedAt);
        if (since != null) {
            query.ge(IndicatorValueEntity::getCreatedAt, since.atStartOfDay());
        }
        List<IndicatorValueEntity> values = indicatorValueMapper.selectList(query);

        if (values.isEmpty()) {
            return List.of();
        }

        // Collect report IDs to fetch report dates
        List<Long> reportIds = values.stream()
                .map(IndicatorValueEntity::getReportId)
                .distinct()
                .toList();
        Map<Long, LocalDate> reportDateMap = labReportMapper.selectList(
                new LambdaQueryWrapper<LabReportEntity>()
                        .in(LabReportEntity::getId, reportIds))
                .stream()
                .collect(Collectors.toMap(LabReportEntity::getId, LabReportEntity::getReportDate, (a, b) -> a));

        return values.stream()
                .map(v -> new TrendPointVo(
                        String.valueOf(v.getReportId()),
                        reportDateMap.getOrDefault(v.getReportId(), v.getCreatedAt().toLocalDate()),
                        v.getValue(),
                        v.getUnit(),
                        v.getAbnormalFlag()))
                .sorted(Comparator.comparing(TrendPointVo::reportDate))
                .toList();
    }

    public TrendSummaryVo getTrendSummary(long patientId, String indicatorCode) {
        dataScopeService.requirePatient(patientId);

        LambdaQueryWrapper<IndicatorValueEntity> query = new LambdaQueryWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getPatientId, patientId)
                        .eq(IndicatorValueEntity::getIndicatorCode, indicatorCode)
                        .eq(IndicatorValueEntity::getManuallyConfirmed, 1)
                        .eq(IndicatorValueEntity::getDeleted, 0)
                        .orderByDesc(IndicatorValueEntity::getCreatedAt);
        LocalDate since = trendSince(0);
        if (since != null) {
            query.ge(IndicatorValueEntity::getCreatedAt, since.atStartOfDay());
        }
        List<IndicatorValueEntity> values = indicatorValueMapper.selectList(query);

        if (values.isEmpty()) {
            throw new BusinessException(ErrorCode.INDICATOR_NOT_FOUND);
        }

        BigDecimal latest = values.get(0).getValue();
        BigDecimal min = values.stream().map(IndicatorValueEntity::getValue).min(BigDecimal::compareTo).orElse(latest);
        BigDecimal max = values.stream().map(IndicatorValueEntity::getValue).max(BigDecimal::compareTo).orElse(latest);
        BigDecimal sum = values.stream().map(IndicatorValueEntity::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        String unit = values.get(0).getUnit();
        String indicatorName = values.get(0).getIndicatorName();
        String trendDirection = computeTrendDirection(values);

        return new TrendSummaryVo(
                indicatorCode,
                indicatorName,
                latest,
                min,
                max,
                average,
                unit,
                trendDirection,
                values.size());
    }

    private String computeTrendDirection(List<IndicatorValueEntity> values) {
        if (values.size() < 2) {
            return "STABLE";
        }
        // Compare the most recent value with the oldest value (list is desc by createdAt)
        BigDecimal newest = values.get(0).getValue();
        BigDecimal oldest = values.get(values.size() - 1).getValue();
        int cmp = newest.compareTo(oldest);
        if (cmp > 0) {
            return "UP";
        } else if (cmp < 0) {
            return "DOWN";
        }
        return "STABLE";
    }

    /** 免费客户只看近三天基础趋势，业务端和平台端维持原有完整历史查询。 */
    private LocalDate trendSince(int months) {
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) {
            return months > 0 ? LocalDate.now().minusMonths(months) : null;
        }
        MembershipEntitlementService.MembershipSnapshot membership =
                membershipEntitlementService.snapshot(current);
        return membership.paidActive()
                ? null
                : LocalDate.now().minusDays(MembershipEntitlementService.FREE_HEALTH_HISTORY_DAYS);
    }
}
