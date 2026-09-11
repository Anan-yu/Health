package com.rayk.health.assistant.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rayk.health.assistant.application.MedicalAssistantApplicationService;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class HealthTreeHoleFeedbackSchedulerTest {
    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void runsEachCandidateAsThatCustomerWithinItsTenant() {
        PatientMapper patientMapper = mock(PatientMapper.class);
        MedicalAssistantApplicationService assistantService =
                mock(MedicalAssistantApplicationService.class);
        PatientEntity patient = new PatientEntity();
        patient.setId(42L);
        patient.setTenantId(9L);
        patient.setUserId(77L);
        when(patientMapper.selectList(any())).thenReturn(List.of(patient));
        when(assistantService.generateTreeHoleFeedbackIfDue(42L))
                .thenAnswer(
                        invocation -> {
                            assertThat(TenantContext.get()).isEqualTo(9L);
                            assertThat(CurrentUser.require().userId()).isEqualTo(77L);
                            assertThat(CurrentUser.require().tenantId()).isEqualTo(9L);
                            assertThat(CurrentUser.require().workbench()).isEqualTo("CUSTOMER");
                            return true;
                        });

        new HealthTreeHoleFeedbackScheduler(patientMapper, assistantService)
                .generateDueFeedback();

        verify(assistantService).generateTreeHoleFeedbackIfDue(42L);
        assertThat(TenantContext.get()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
