package com.rayk.health.patient.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DataScopeServiceTest {
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PatientEntity.class);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void doctorSearchesAllActiveCustomersAcrossTenants() {
        CurrentPrincipal principal = new CurrentPrincipal("jti", "doctor", 10003L, 20001L,
                List.of("DOCTOR"), List.of("patient:list"), "DOCTOR");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
        LambdaQueryWrapper<PatientEntity> query = new DataScopeService(org.mockito.Mockito.mock(PatientMapper.class)).scopedPatients();
        String sql = query.getSqlSegment();
        assertThat(sql).contains("deleted").doesNotContain("tenant_id");
        assertThat(sql).doesNotContain("privacy_consent").doesNotContain("DATA_SHARING");
        assertThat(sql).doesNotContain("assigned_doctor_id");
    }

    @Test
    void customerRemainsRestrictedToOwnTenantAndUser() {
        CurrentPrincipal principal = new CurrentPrincipal("jti", "customer", 10005L, 20001L,
                List.of("CUSTOMER"), List.of("self:profile"), "CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
        LambdaQueryWrapper<PatientEntity> query =
                new DataScopeService(org.mockito.Mockito.mock(PatientMapper.class)).scopedPatients();
        String sql = query.getSqlSegment();
        assertThat(sql).contains("tenant_id").contains("user_id").contains("deleted");
    }
}
