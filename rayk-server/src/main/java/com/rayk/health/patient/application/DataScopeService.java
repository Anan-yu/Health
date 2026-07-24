package com.rayk.health.patient.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.TenantContext;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Applies the customer data boundary for each workbench role. */
@Service
public class DataScopeService {
    private final PatientMapper patientMapper;

    public DataScopeService(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    public LambdaQueryWrapper<PatientEntity> scopedPatients() {
        CurrentPrincipal current = CurrentUser.require();
        LambdaQueryWrapper<PatientEntity> query = new LambdaQueryWrapper<PatientEntity>()
                .eq(PatientEntity::getDeleted, 0);
        switch (current.workbench()) {
            case "CUSTOMER" -> query
                    .eq(PatientEntity::getTenantId, current.tenantId())
                    .eq(PatientEntity::getUserId, current.userId());
            // During the initial rollout doctors use the platform-wide examinee pool and keep
            // read-only permissions. Name/phone search is provided by the patient list endpoint.
            case "DOCTOR" -> {
                // The deletion predicate above is the complete doctor data boundary.
            }
            case "PLATFORM_ADMIN" -> {
                // Platform administrators can view platform-wide reports and follow-up progress.
            }
            default -> query
                    .eq(PatientEntity::getTenantId, current.tenantId())
                    .eq(PatientEntity::getUserId, current.userId());
        }
        return query;
    }

    public <T> T readScoped(Supplier<T> action) {
        String workbench = CurrentUser.require().workbench();
        if ("DOCTOR".equals(workbench) || "PLATFORM_ADMIN".equals(workbench)) {
            return TenantContext.executeReadWithoutTenant(action);
        }
        return action.get();
    }

    public PatientEntity requirePatient(long patientId) {
        PatientEntity patient = readScoped(
                () -> patientMapper.selectOne(
                        scopedPatients().eq(PatientEntity::getId, patientId)));
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return patient;
    }
}
