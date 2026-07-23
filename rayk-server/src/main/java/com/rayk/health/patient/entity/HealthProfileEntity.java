package com.rayk.health.patient.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("health_profile")
public class HealthProfileEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long patientId;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal bmi;
    private String bloodType;
    private String lifestyleSummary;
    private String medicalHistory;
    private String familyHistory;
    private String allergyHistory;
    private String currentMedications;
    private String smokingStatus;
    private String alcoholStatus;
    private String exerciseFrequency;
    private String sleepQuality;
    private BigDecimal sleepHours;
    private String stressLevel;
    private String moodStatus;
    private String fearLevel;
    private String dietaryPreference;
    private String recentDietaryPattern;
    private String diabetesStatus;
    private String hypertensionStatus;
    private String dyslipidemiaStatus;
    private String fattyLiverStatus;
    private Integer profileCompleteness;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic private Integer deleted;
    private Integer version;

    /**
     * 根据身高体重计算 BMI，若身高或体重为空则不计算。
     */
    public void recalculateBmi() {
        if (heightCm != null && weightKg != null
                && heightCm.compareTo(BigDecimal.ZERO) > 0
                && weightKg.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            this.bmi = weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
        } else {
            this.bmi = null;
        }
    }
}
