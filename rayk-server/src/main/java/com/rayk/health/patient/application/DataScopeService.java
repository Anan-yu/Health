package com.rayk.health.patient.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import org.springframework.stereotype.Service;

/**
 * 数据范围服务：在租户隔离（由 MyBatis-Plus TenantLineInnerInterceptor 自动追加）之上，
 * 按工作台角色进行二次行级收窄。
 *
 * <p>纵深防御：即使拦截器已自动追加 tenant_id 条件，此处仍显式添加，
 * 防止拦截器被误配置或禁用时产生跨租户泄漏。
 */
@Service
public class DataScopeService {
    private static final String ACTIVE_DATA_SHARING_CONSENT_SQL =
            "SELECT patient_id FROM privacy_consent "
                    + "WHERE tenant_id = %d AND consent_type = 'DATA_SHARING' "
                    + "AND consented = 1 AND deleted = 0";

    private final PatientMapper patientMapper;

    public DataScopeService(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    /**
     * 返回当前用户可访问的客户查询条件。
     *
     * <p>规则：
     * <ul>
     *   <li>所有角色：首先限定 tenant_id（纵深防御）和 deleted=0</li>
     *   <li>CUSTOMER：只能看到自己的客户档案</li>
     *   <li>DOCTOR：只能看到分配给自己且已授权专业协作的客户</li>
     *   <li>HEALTH_MANAGER：只能看到分配给自己且已授权专业协作的客户</li>
     *   <li>TENANT_ADMIN：可看到本租户已授权专业协作的客户</li>
     *   <li>PLATFORM_ADMIN：可看到所有租户客户（用于平台审计，拦截器按其 tenantId=1 过滤）</li>
     * </ul>
     */
    public LambdaQueryWrapper<PatientEntity> scopedPatients() {
        CurrentPrincipal current = CurrentUser.require();
        LambdaQueryWrapper<PatientEntity> query =
                new LambdaQueryWrapper<PatientEntity>()
                        .eq(PatientEntity::getTenantId, current.tenantId())
                        .eq(PatientEntity::getDeleted, 0);
        switch (current.workbench()) {
            case "CUSTOMER" -> query.eq(PatientEntity::getUserId, current.userId());
            case "DOCTOR" -> query.eq(PatientEntity::getAssignedDoctorId, current.userId())
                    .inSql(
                            PatientEntity::getId,
                            ACTIVE_DATA_SHARING_CONSENT_SQL.formatted(current.tenantId()));
            case "HEALTH_MANAGER" -> query.eq(PatientEntity::getAssignedManagerId, current.userId())
                    .inSql(
                            PatientEntity::getId,
                            ACTIVE_DATA_SHARING_CONSENT_SQL.formatted(current.tenantId()));
            case "TENANT_ADMIN" -> {
                // 机构管理员也只能查看已授权专业人员协作的客户。
                query.inSql(
                        PatientEntity::getId,
                        ACTIVE_DATA_SHARING_CONSENT_SQL.formatted(current.tenantId()));
            }
            case "PLATFORM_ADMIN" -> {
                // 平台管理员：租户条件已限定（tenantId=1），平台审计场景如需跨租户
                // 应通过专用平台接口并显式设置 TenantContext
            }
            default -> {
                // 未知工作台：保守处理，仅允许访问自己的客户档案
                query.eq(PatientEntity::getUserId, current.userId());
            }
        }
        return query;
    }

    /**
     * 按 ID 获取当前用户可访问的客户，不存在或越权时抛出业务异常。
     */
    public PatientEntity requirePatient(long patientId) {
        PatientEntity patient =
                patientMapper.selectOne(scopedPatients().eq(PatientEntity::getId, patientId));
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return patient;
    }
}
