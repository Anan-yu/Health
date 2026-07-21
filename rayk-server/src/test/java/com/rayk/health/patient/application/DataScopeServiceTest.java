package com.rayk.health.patient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DataScopeServiceTest {
    private PatientMapper patientMapper;
    private DataScopeService dataScopeService;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), PatientEntity.class);
    }

    @BeforeEach
    void setUp() {
        patientMapper = mock(PatientMapper.class);
        dataScopeService = new DataScopeService(patientMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(long userId, long tenantId, String workbench) {
        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "jti-test",
                        "user_" + userId,
                        userId,
                        tenantId,
                        List.of("TEST_ROLE"),
                        List.of("patient:list"),
                        workbench);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @Test
    void scopedPatientsIncludesTenantIdFilter() {
        authenticateAs(10002, 20001, "TENANT_ADMIN");
        LambdaQueryWrapper<PatientEntity> wrapper = dataScopeService.scopedPatients();
        // 验证 wrapper 不为空且包含条件（通过 SQL 表达式检查）
        assertThat(wrapper).isNotNull();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("tenant_id");
        assertThat(sqlSegment).contains("deleted");
    }

    @Test
    void customerWorkbenchFiltersByUserId() {
        authenticateAs(10005, 20001, "CUSTOMER");
        LambdaQueryWrapper<PatientEntity> wrapper = dataScopeService.scopedPatients();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("tenant_id");
        assertThat(sqlSegment).contains("user_id");
        assertThat(sqlSegment).doesNotContain("privacy_consent");
    }

    @Test
    void doctorWorkbenchFiltersByAssignedDoctorId() {
        authenticateAs(10003, 20001, "DOCTOR");
        LambdaQueryWrapper<PatientEntity> wrapper = dataScopeService.scopedPatients();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("tenant_id");
        assertThat(sqlSegment).contains("assigned_doctor_id");
        assertThat(sqlSegment).contains("privacy_consent");
        assertThat(sqlSegment).contains("DATA_SHARING");
    }

    @Test
    void healthManagerWorkbenchFiltersByAssignedManagerId() {
        authenticateAs(10004, 20001, "HEALTH_MANAGER");
        LambdaQueryWrapper<PatientEntity> wrapper = dataScopeService.scopedPatients();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("tenant_id");
        assertThat(sqlSegment).contains("assigned_manager_id");
        assertThat(sqlSegment).contains("privacy_consent");
        assertThat(sqlSegment).contains("DATA_SHARING");
    }

    @Test
    void tenantAdminWorkbenchRequiresActiveDataSharingConsent() {
        authenticateAs(10002, 20001, "TENANT_ADMIN");
        LambdaQueryWrapper<PatientEntity> wrapper = dataScopeService.scopedPatients();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("tenant_id");
        // TENANT_ADMIN 不应有 user_id / assigned_doctor_id / assigned_manager_id 条件
        assertThat(sqlSegment).doesNotContain("user_id");
        assertThat(sqlSegment).doesNotContain("assigned_doctor_id");
        assertThat(sqlSegment).doesNotContain("assigned_manager_id");
        assertThat(sqlSegment).contains("privacy_consent");
        assertThat(sqlSegment).contains("DATA_SHARING");
    }

    @Test
    void unknownWorkbenchFallsBackToUserIdFilter() {
        authenticateAs(99999, 20001, "UNKNOWN_WORKBENCH");
        LambdaQueryWrapper<PatientEntity> wrapper = dataScopeService.scopedPatients();
        String sqlSegment = wrapper.getSqlSegment();
        assertThat(sqlSegment).contains("tenant_id");
        assertThat(sqlSegment).contains("user_id");
    }

    @Test
    void requirePatientThrowsWhenNotFound() {
        authenticateAs(10005, 20001, "CUSTOMER");
        when(patientMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> dataScopeService.requirePatient(99999))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void requirePatientReturnsEntityWhenFound() {
        authenticateAs(10002, 20001, "TENANT_ADMIN");
        PatientEntity patient = new PatientEntity();
        patient.setId(30001L);
        patient.setTenantId(20001L);
        patient.setName("测试客户");
        when(patientMapper.selectOne(any())).thenReturn(patient);
        PatientEntity result = dataScopeService.requirePatient(30001L);
        assertThat(result.getId()).isEqualTo(30001L);
        assertThat(result.getTenantId()).isEqualTo(20001L);
    }

    @Test
    void differentTenantsProduceDifferentTenantIdInWrapper() {
        authenticateAs(10002, 20001, "TENANT_ADMIN");
        LambdaQueryWrapper<PatientEntity> wrapperA = dataScopeService.scopedPatients();
        String sqlA = wrapperA.getSqlSegment();

        authenticateAs(60001, 20002, "TENANT_ADMIN");
        LambdaQueryWrapper<PatientEntity> wrapperB = dataScopeService.scopedPatients();
        String sqlB = wrapperB.getSqlSegment();

        // 两个 wrapper 都包含 tenant_id 和 deleted 条件
        assertThat(sqlA).contains("tenant_id");
        assertThat(sqlA).contains("deleted");
        assertThat(sqlB).contains("tenant_id");
        assertThat(sqlB).contains("deleted");
        // 授权子查询也必须显式使用当前租户，不能跨租户共享授权。
        assertThat(sqlA).contains("privacy_consent").contains("20001");
        assertThat(sqlB).contains("privacy_consent").contains("20002");
        assertThat(sqlA).isNotEqualTo(sqlB);
    }
}
