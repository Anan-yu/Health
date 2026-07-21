package com.rayk.health.patient.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.patient.dto.UpdateProfileRequest;
import com.rayk.health.patient.entity.HealthProfileEntity;
import com.rayk.health.patient.mapper.HealthProfileMapper;
import com.rayk.health.patient.vo.HealthProfileVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.application.PrivacyConsentService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class HealthProfileService {
    private final HealthProfileMapper profileMapper;
    private final DataScopeService dataScopeService;
    private final PrivacyConsentService privacyConsentService;

    public HealthProfileService(
            HealthProfileMapper profileMapper,
            DataScopeService dataScopeService,
            PrivacyConsentService privacyConsentService) {
        this.profileMapper = profileMapper;
        this.dataScopeService = dataScopeService;
        this.privacyConsentService = privacyConsentService;
    }

    /**
     * 获取客户健康档案，不存在则自动创建空档案。
     */
    public HealthProfileVo getProfile(long patientId) {
        dataScopeService.requirePatient(patientId);
        HealthProfileEntity entity = findOrCreate(patientId);
        return toVo(entity);
    }

    /**
     * 更新客户健康档案，重新计算 BMI 和完整度。
     */
    public HealthProfileVo updateProfile(long patientId, UpdateProfileRequest request) {
        dataScopeService.requirePatient(patientId);
        privacyConsentService.requireConsent(
                patientId, PrivacyConsentService.TYPE_DATA_COLLECTION);
        CurrentPrincipal current = CurrentUser.require();
        HealthProfileEntity entity = findOrCreate(patientId);

        if (request.heightCm() != null) entity.setHeightCm(request.heightCm());
        if (request.weightKg() != null) entity.setWeightKg(request.weightKg());
        if (request.bloodType() != null) entity.setBloodType(request.bloodType());
        if (request.lifestyleSummary() != null) entity.setLifestyleSummary(request.lifestyleSummary());
        if (request.medicalHistory() != null) entity.setMedicalHistory(request.medicalHistory());
        if (request.allergyHistory() != null) entity.setAllergyHistory(request.allergyHistory());
        if (request.currentMedications() != null) entity.setCurrentMedications(request.currentMedications());
        if (request.smokingStatus() != null) entity.setSmokingStatus(request.smokingStatus());
        if (request.alcoholStatus() != null) entity.setAlcoholStatus(request.alcoholStatus());
        if (request.exerciseFrequency() != null) entity.setExerciseFrequency(request.exerciseFrequency());
        if (request.sleepQuality() != null) entity.setSleepQuality(request.sleepQuality());
        if (request.stressLevel() != null) entity.setStressLevel(request.stressLevel());
        if (request.dietaryPreference() != null) entity.setDietaryPreference(request.dietaryPreference());

        entity.recalculateBmi();
        entity.setProfileCompleteness(ProfileCompletenessCalculator.calculate(entity));
        entity.setUpdatedBy(current.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        profileMapper.updateById(entity);

        return toVo(entity);
    }

    private HealthProfileEntity findOrCreate(long patientId) {
        CurrentPrincipal current = CurrentUser.require();
        HealthProfileEntity entity = profileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfileEntity>()
                        .eq(HealthProfileEntity::getTenantId, current.tenantId())
                        .eq(HealthProfileEntity::getPatientId, patientId)
                        .eq(HealthProfileEntity::getDeleted, 0));
        if (entity == null) {
            entity = new HealthProfileEntity();
            entity.setTenantId(current.tenantId());
            entity.setPatientId(patientId);
            entity.setProfileCompleteness(0);
            entity.setCreatedBy(current.userId());
            entity.setUpdatedBy(current.userId());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDeleted(0);
            entity.setVersion(0);
            profileMapper.insert(entity);
        }
        return entity;
    }

    private HealthProfileVo toVo(HealthProfileEntity entity) {
        return new HealthProfileVo(
                String.valueOf(entity.getId()),
                String.valueOf(entity.getPatientId()),
                entity.getHeightCm(),
                entity.getWeightKg(),
                entity.getBmi(),
                entity.getBloodType(),
                entity.getLifestyleSummary(),
                entity.getMedicalHistory(),
                entity.getAllergyHistory(),
                entity.getCurrentMedications(),
                entity.getSmokingStatus(),
                entity.getAlcoholStatus(),
                entity.getExerciseFrequency(),
                entity.getSleepQuality(),
                entity.getStressLevel(),
                entity.getDietaryPreference(),
                entity.getProfileCompleteness() == null ? 0 : entity.getProfileCompleteness(),
                entity.getUpdatedAt());
    }
}
