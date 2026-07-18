package com.rayk.health.patient.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class DataScopeService {
    private final PatientMapper patientMapper;

    public DataScopeService(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    public LambdaQueryWrapper<PatientEntity> scopedPatients() {
        CurrentPrincipal current = CurrentUser.require();
        LambdaQueryWrapper<PatientEntity> query =
                new LambdaQueryWrapper<PatientEntity>().eq(PatientEntity::getDeleted, 0);
        if ("CUSTOMER".equals(current.workbench())) {
            query.eq(PatientEntity::getUserId, current.userId());
        } else if ("DOCTOR".equals(current.workbench())) {
            query.eq(PatientEntity::getAssignedDoctorId, current.userId());
        } else if ("HEALTH_MANAGER".equals(current.workbench())) {
            query.eq(PatientEntity::getAssignedManagerId, current.userId());
        }
        return query;
    }

    public PatientEntity requirePatient(long patientId) {
        PatientEntity patient =
                patientMapper.selectOne(scopedPatients().eq(PatientEntity::getId, patientId));
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return patient;
    }
}

