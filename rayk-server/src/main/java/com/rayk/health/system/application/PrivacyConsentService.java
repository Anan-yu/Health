package com.rayk.health.system.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.aspect.Audited;
import com.rayk.health.system.entity.PrivacyConsentEntity;
import com.rayk.health.system.mapper.PrivacyConsentMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PrivacyConsentService {

    /** 支持的授权类型 */
    public static final String TYPE_DATA_COLLECTION = "DATA_COLLECTION";
    public static final String TYPE_HEALTH_ASSESSMENT = "HEALTH_ASSESSMENT";
    public static final String TYPE_DATA_SHARING = "DATA_SHARING";
    public static final String TYPE_MARKETING = "MARKETING";

    private final PrivacyConsentMapper consentMapper;
    private final DataScopeService dataScopeService;

    public PrivacyConsentService(
            PrivacyConsentMapper consentMapper, DataScopeService dataScopeService) {
        this.consentMapper = consentMapper;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 授予隐私授权。若已存在同类型有效授权则更新政策版本，否则新建记录。
     */
    @Audited(operationType = "GRANT_PRIVACY_CONSENT", resourceType = "PRIVACY_CONSENT")
    public PrivacyConsentEntity grantConsent(long patientId, String consentType, String policyVersion) {
        dataScopeService.requirePatient(patientId);
        CurrentPrincipal current = CurrentUser.require();
        PrivacyConsentEntity existing = findActive(patientId, consentType, current.tenantId());

        if (existing != null) {
            existing.setPolicyVersion(policyVersion);
            existing.setConsented(1);
            existing.setConsentedAt(LocalDateTime.now());
            existing.setRevokedAt(null);
            existing.setUpdatedBy(current.userId());
            existing.setUpdatedAt(LocalDateTime.now());
            consentMapper.updateById(existing);
            return existing;
        }

        PrivacyConsentEntity entity = new PrivacyConsentEntity();
        entity.setTenantId(current.tenantId());
        entity.setPatientId(patientId);
        entity.setConsentType(consentType);
        entity.setPolicyVersion(policyVersion);
        entity.setConsented(1);
        entity.setConsentedAt(LocalDateTime.now());
        entity.setCreatedBy(current.userId());
        entity.setUpdatedBy(current.userId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setVersion(0);
        consentMapper.insert(entity);
        return entity;
    }

    /**
     * 撤回隐私授权。
     */
    @Audited(operationType = "REVOKE_PRIVACY_CONSENT", resourceType = "PRIVACY_CONSENT")
    public void revokeConsent(long patientId, String consentType) {
        dataScopeService.requirePatient(patientId);
        CurrentPrincipal current = CurrentUser.require();
        PrivacyConsentEntity existing = findActive(patientId, consentType, current.tenantId());
        if (existing != null) {
            existing.setConsented(0);
            existing.setRevokedAt(LocalDateTime.now());
            existing.setUpdatedBy(current.userId());
            existing.setUpdatedAt(LocalDateTime.now());
            consentMapper.updateById(existing);
        }
    }

    /**
     * 检查指定类型的授权是否有效。
     */
    public boolean checkConsent(long patientId, String consentType) {
        dataScopeService.requirePatient(patientId);
        CurrentPrincipal current = CurrentUser.require();
        return findActive(patientId, consentType, current.tenantId()) != null;
    }

    /**
     * 要求指定授权处于有效状态。业务服务在写入或处理健康数据前调用，确保撤回授权后
     * 不会继续创建新的数据处理任务。
     */
    public void requireConsent(long patientId, String consentType) {
        if (checkConsent(patientId, consentType)) {
            return;
        }
        // Health-service consent is recorded automatically on first use. The mini-program no
        // longer presents a separate authorization-card workflow that can block core services.
        grantConsent(patientId, consentType, "2026.07");
    }

    /**
     * 列出客户所有授权记录。
     */
    public List<PrivacyConsentEntity> listConsents(long patientId) {
        dataScopeService.requirePatient(patientId);
        CurrentPrincipal current = CurrentUser.require();
        return consentMapper.selectList(
                new LambdaQueryWrapper<PrivacyConsentEntity>()
                        .eq(PrivacyConsentEntity::getTenantId, current.tenantId())
                        .eq(PrivacyConsentEntity::getPatientId, patientId)
                        .eq(PrivacyConsentEntity::getDeleted, 0)
                        .orderByDesc(PrivacyConsentEntity::getCreatedAt));
    }

    private PrivacyConsentEntity findActive(long patientId, String consentType, long tenantId) {
        return consentMapper.selectOne(
                new LambdaQueryWrapper<PrivacyConsentEntity>()
                        .eq(PrivacyConsentEntity::getTenantId, tenantId)
                        .eq(PrivacyConsentEntity::getPatientId, patientId)
                        .eq(PrivacyConsentEntity::getConsentType, consentType)
                        .eq(PrivacyConsentEntity::getConsented, 1)
                        .eq(PrivacyConsentEntity::getDeleted, 0)
                        .last("LIMIT 1"));
    }
}
