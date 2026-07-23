package com.rayk.health.patient.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
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
                .eq(PatientEntity::getTenantId, current.tenantId())
                .eq(PatientEntity::getDeleted, 0);
        switch (current.workbench()) {
            case "CUSTOMER" -> query.eq(PatientEntity::getUserId, current.userId());
            // Doctors use the same-hospital examinee pool. Their permissions remain read-only:
            // they can search customers and view published reports, but cannot edit or publish them.
            case "DOCTOR" -> {
                // Tenant and deletion predicates above are the complete doctor data boundary.
            }
            case "PLATFORM_ADMIN" -> {
                // Platform-wide statistics and hospital management are provided through dedicated APIs.
            }
            default -> query.eq(PatientEntity::getUserId, current.userId());
        }
        return query;
    }

    public PatientEntity requirePatient(long patientId) {
        PatientEntity patient = patientMapper.selectOne(scopedPatients().eq(PatientEntity::getId, patientId));
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return patient;
    }
}
