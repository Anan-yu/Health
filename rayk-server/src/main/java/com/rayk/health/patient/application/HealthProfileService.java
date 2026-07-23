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
        refreshDerivedValues(entity, false);
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

        // PUT replaces the complete profile. Null or blank values must clear old data so the
        // completeness percentage can decrease after a user removes a field.
        entity.setHeightCm(request.heightCm());
        entity.setWeightKg(request.weightKg());
        entity.setBloodType(normalize(request.bloodType()));
        entity.setLifestyleSummary(normalize(request.lifestyleSummary()));
        entity.setMedicalHistory(normalize(request.medicalHistory()));
        entity.setFamilyHistory(normalize(request.familyHistory()));
        entity.setAllergyHistory(normalize(request.allergyHistory()));
        entity.setCurrentMedications(normalize(request.currentMedications()));
        entity.setSmokingStatus(normalize(request.smokingStatus()));
        entity.setAlcoholStatus(normalize(request.alcoholStatus()));
        entity.setExerciseFrequency(normalize(request.exerciseFrequency()));
        entity.setSleepQuality(normalize(request.sleepQuality()));
        entity.setSleepHours(request.sleepHours());
        entity.setStressLevel(normalize(request.stressLevel()));
        entity.setMoodStatus(normalize(request.moodStatus()));
        entity.setFearLevel(normalize(request.fearLevel()));
        entity.setDietaryPreference(normalize(request.dietaryPreference()));
        entity.setRecentDietaryPattern(normalize(request.recentDietaryPattern()));
        entity.setDiabetesStatus(normalize(request.diabetesStatus()));
        entity.setHypertensionStatus(normalize(request.hypertensionStatus()));
        entity.setDyslipidemiaStatus(normalize(request.dyslipidemiaStatus()));
        entity.setFattyLiverStatus(normalize(request.fattyLiverStatus()));

        entity.setUpdatedBy(current.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        refreshDerivedValues(entity, true);

        return toVo(entity);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    /**
     * Keeps older seeded/imported records consistent with the current questionnaire fields.
     * Completeness is derived, never trusted from a previously stored percentage.
     */
    private void refreshDerivedValues(HealthProfileEntity entity, boolean forcePersist) {
        entity.recalculateBmi();
        int calculatedCompleteness = ProfileCompletenessCalculator.calculate(entity);
        boolean changed = entity.getProfileCompleteness() == null
                || entity.getProfileCompleteness() != calculatedCompleteness;
        entity.setProfileCompleteness(calculatedCompleteness);
        if (forcePersist || changed) {
            profileMapper.updateById(entity);
        }
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
                entity.getFamilyHistory(),
                entity.getAllergyHistory(),
                entity.getCurrentMedications(),
                entity.getSmokingStatus(),
                entity.getAlcoholStatus(),
                entity.getExerciseFrequency(),
                entity.getSleepQuality(),
                entity.getSleepHours(),
                entity.getStressLevel(),
                entity.getMoodStatus(),
                entity.getFearLevel(),
                entity.getDietaryPreference(),
                entity.getRecentDietaryPattern(),
                entity.getDiabetesStatus(),
                entity.getHypertensionStatus(),
                entity.getDyslipidemiaStatus(),
                entity.getFattyLiverStatus(),
                entity.getProfileCompleteness() == null ? 0 : entity.getProfileCompleteness(),
                entity.getUpdatedAt());
    }
}
