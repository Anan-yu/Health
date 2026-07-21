package com.rayk.health.system.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.system.entity.PrivacyConsentEntity;
import com.rayk.health.system.mapper.PrivacyConsentMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PrivacyConsentServiceTest {
    private PrivacyConsentMapper consentMapper;
    private PrivacyConsentService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                PrivacyConsentEntity.class);
    }

    @BeforeEach
    void setUp() {
        consentMapper = mock(PrivacyConsentMapper.class);
        DataScopeService dataScopeService = mock(DataScopeService.class);
        PatientEntity patient = new PatientEntity();
        patient.setId(30001L);
        when(dataScopeService.requirePatient(30001L)).thenReturn(patient);
        service = new PrivacyConsentService(consentMapper, dataScopeService);

        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "consent-test",
                        "customer",
                        10005L,
                        20001L,
                        List.of("CUSTOMER"),
                        List.of("self:health-record"),
                        "CUSTOMER");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsActiveConsent() {
        PrivacyConsentEntity consent = new PrivacyConsentEntity();
        consent.setConsented(1);
        when(consentMapper.selectOne(any())).thenReturn(consent);

        assertThatCode(
                        () ->
                                service.requireConsent(
                                        30001L, PrivacyConsentService.TYPE_DATA_COLLECTION))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingAssessmentConsentWithSpecificCode() {
        when(consentMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(
                        () ->
                                service.requireConsent(
                                        30001L, PrivacyConsentService.TYPE_HEALTH_ASSESSMENT))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                exception.getErrorCode())
                                        .isEqualTo(
                                                ErrorCode.HEALTH_ASSESSMENT_CONSENT_REQUIRED));
    }
}
