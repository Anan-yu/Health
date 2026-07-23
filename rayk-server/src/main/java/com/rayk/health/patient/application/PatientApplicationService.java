package com.rayk.health.patient.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.patient.converter.PatientConverter;
import com.rayk.health.patient.dto.CreatePatientRequest;
import com.rayk.health.patient.dto.UpdatePatientIdentityRequest;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.patient.vo.PatientVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PatientApplicationService {
    private final PatientMapper patientMapper;
    private final PatientConverter converter;
    private final DataScopeService dataScopeService;

    public PatientApplicationService(
            PatientMapper patientMapper,
            PatientConverter converter,
            DataScopeService dataScopeService) {
        this.patientMapper = patientMapper;
        this.converter = converter;
        this.dataScopeService = dataScopeService;
    }

    public List<PatientVo> list(String keyword) {
        LambdaQueryWrapper<PatientEntity> query =
                dataScopeService.scopedPatients().orderByDesc(PatientEntity::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            if (value.matches("^1[3-9]\\d{9}$")) {
                query.eq(PatientEntity::getPhoneHash, PhoneIdentity.hash(value));
            } else {
                query.and(wrapper -> wrapper.like(PatientEntity::getName, value)
                        .or().like(PatientEntity::getPhoneMasked, value));
            }
        }
        return patientMapper.selectList(query).stream().map(converter::toVo).toList();
    }

    public PatientVo get(long id) {
        return converter.toVo(dataScopeService.requirePatient(id));
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public PatientVo updateSelfIdentity(long id, UpdatePatientIdentityRequest request) {
        PatientEntity entity = dataScopeService.requirePatient(id);
        CurrentPrincipal current = CurrentUser.require();
        entity.setName(request.name().trim());
        entity.setGender(request.gender());
        entity.setBirthDate(request.birthDate());
        if (request.phone() != null && !request.phone().isBlank()) {
            String phone = PhoneIdentity.normalize(request.phone());
            entity.setPhoneMasked(maskPhone(phone));
            entity.setPhoneHash(PhoneIdentity.hash(phone));
        }
        entity.setUpdatedBy(current.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        patientMapper.updateById(entity);
        return converter.toVo(entity);
    }

    @PreAuthorize("hasAuthority('patient:create') or (hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER')")
    public PatientVo create(CreatePatientRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity entity = new PatientEntity();
        entity.setTenantId(current.tenantId());
        entity.setUserId("CUSTOMER".equals(current.workbench()) ? current.userId() : null);
        entity.setName(request.name());
        entity.setGender(request.gender() == null ? "UNKNOWN" : request.gender());
        entity.setBirthDate(request.birthDate());
        entity.setPhoneMasked(maskPhone(request.phone()));
        entity.setPhoneHash(request.phone() != null && request.phone().matches("^1[3-9]\\d{9}$")
                ? PhoneIdentity.hash(request.phone()) : null);
        entity.setAssignedDoctorId(request.assignedDoctorId());
        entity.setAssignedManagerId(request.assignedManagerId());
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(current.userId());
        entity.setUpdatedBy(current.userId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setVersion(0);
        patientMapper.insert(entity);
        return converter.toVo(entity);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
