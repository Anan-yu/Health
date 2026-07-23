package com.rayk.health.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.assessment.mapper.HealthAssessmentMapper;
import com.rayk.health.followup.entity.FollowupTaskEntity;
import com.rayk.health.followup.mapper.FollowupTaskMapper;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.indicator.mapper.IndicatorValueMapper;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.report.entity.HealthReportEntity;
import com.rayk.health.report.mapper.HealthReportMapper;
import com.rayk.health.review.entity.AssessmentReviewEntity;
import com.rayk.health.review.mapper.AssessmentReviewMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.system.entity.PrivacyConsentEntity;
import com.rayk.health.system.mapper.PrivacyConsentMapper;
import com.rayk.health.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 多租户隔离集成测试。
 *
 * <p>验证 MyBatis-Plus TenantLineInnerInterceptor 正确阻止跨租户数据访问。
 * 租户 A (20001) 和租户 B (20002) 各自拥有独立的客户、报告、指标、随访、评估和审核数据。
 */
@SpringBootTest(
        properties = {
            "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
@ActiveProfiles("test")
@Import(TestExternalServicesConfig.class)
class TenantIsolationIntegrationTest {
    private static final long TENANT_A = 20001L;
    private static final long TENANT_B = 20002L;
    private static final long TENANT_A_PATIENT = 30001L;
    private static final long TENANT_B_PATIENT = 40001L;
    private static final long TENANT_A_REPORT = 50001L;
    private static final long TENANT_B_REPORT = 60001L;

    @Autowired private PatientMapper patientMapper;
    @Autowired private DataScopeService dataScopeService;
    @Autowired private PrivacyConsentMapper privacyConsentMapper;
    @Autowired private LabReportMapper labReportMapper;
    @Autowired private IndicatorValueMapper indicatorMapper;
    @Autowired private FollowupTaskMapper followupMapper;
    @Autowired private HealthAssessmentMapper assessmentMapper;
    @Autowired private AssessmentReviewMapper reviewMapper;
    @Autowired private HealthReportMapper healthReportMapper;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateAsTenant(long tenantId, long userId, String workbench) {
        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "jti-test",
                        "user_" + userId,
                        userId,
                        tenantId,
                        List.of("TEST_ROLE"),
                        List.of("patient:list", "lab-report:manage", "assessment:review"),
                        workbench);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @Nested
    @DisplayName("客户数据隔离")
    class PatientIsolation {
        @Test
        @DisplayName("租户A只能查到自己的客户")
        void tenantASeesOnlyOwnPatients() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<PatientEntity> patients = patientMapper.selectList(null);
            assertThat(patients).isNotEmpty();
            assertThat(patients).allSatisfy(p -> assertThat(p.getTenantId()).isEqualTo(TENANT_A));
            assertThat(patients).noneSatisfy(p -> assertThat(p.getTenantId()).isEqualTo(TENANT_B));
        }

        @Test
        @DisplayName("租户B只能查到自己的客户")
        void tenantBSeesOnlyOwnPatients() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            List<PatientEntity> patients = patientMapper.selectList(null);
            assertThat(patients).isNotEmpty();
            assertThat(patients).allSatisfy(p -> assertThat(p.getTenantId()).isEqualTo(TENANT_B));
        }

        @Test
        @DisplayName("租户A无法通过ID直接访问租户B的客户")
        void tenantACannotAccessTenantBPatientById() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            PatientEntity patient = patientMapper.selectById(TENANT_B_PATIENT);
            assertThat(patient).isNull();
        }

        @Test
        @DisplayName("租户B无法通过ID直接访问租户A的客户")
        void tenantBCannotAccessTenantAPatientById() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            PatientEntity patient = patientMapper.selectById(TENANT_A_PATIENT);
            assertThat(patient).isNull();
        }

        @Test
        @DisplayName("使用TenantContext也能正确隔离")
        void tenantContextProvidesIsolation() {
            TenantContext.set(TENANT_A);
            List<PatientEntity> patients = patientMapper.selectList(null);
            assertThat(patients).allSatisfy(p -> assertThat(p.getTenantId()).isEqualTo(TENANT_A));
            TenantContext.clear();

            TenantContext.set(TENANT_B);
            patients = patientMapper.selectList(null);
            assertThat(patients).allSatisfy(p -> assertThat(p.getTenantId()).isEqualTo(TENANT_B));
        }

        @Test
        @Transactional
        @DisplayName("医生可查看同院体检者且不依赖已停用的隐私授权入口")
        void doctorScopeUsesHospitalBoundaryInsteadOfLegacyConsent() {
            authenticateAsTenant(TENANT_A, 10003, "DOCTOR");
            assertThat(patientMapper.selectList(dataScopeService.scopedPatients()))
                    .extracting(PatientEntity::getId)
                    .contains(TENANT_A_PATIENT);

            PrivacyConsentEntity consent = privacyConsentMapper.selectById(41001L);
            consent.setConsented(0);
            privacyConsentMapper.updateById(consent);

            assertThat(patientMapper.selectList(dataScopeService.scopedPatients()))
                    .extracting(PatientEntity::getId)
                    .contains(TENANT_A_PATIENT);
        }
    }

    @Nested
    @DisplayName("检验报告隔离")
    class LabReportIsolation {
        @Test
        @DisplayName("租户A只能查到自己的报告")
        void tenantASeesOnlyOwnReports() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<LabReportEntity> reports = labReportMapper.selectList(null);
            assertThat(reports).isNotEmpty();
            assertThat(reports).allSatisfy(r -> assertThat(r.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("租户A无法通过ID访问租户B的报告")
        void tenantACannotAccessTenantBReport() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            LabReportEntity report = labReportMapper.selectById(TENANT_B_REPORT);
            assertThat(report).isNull();
        }

        @Test
        @DisplayName("租户B无法通过ID访问租户A的报告")
        void tenantBCannotAccessTenantAReport() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            LabReportEntity report = labReportMapper.selectById(TENANT_A_REPORT);
            assertThat(report).isNull();
        }
    }

    @Nested
    @DisplayName("指标值隔离")
    class IndicatorIsolation {
        @Test
        @DisplayName("租户A只能查到自己的指标")
        void tenantASeesOnlyOwnIndicators() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<IndicatorValueEntity> indicators = indicatorMapper.selectList(null);
            assertThat(indicators).isNotEmpty();
            assertThat(indicators).allSatisfy(i -> assertThat(i.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("租户B无法看到租户A的指标")
        void tenantBCannotSeeTenantAIndicators() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            List<IndicatorValueEntity> indicators =
                    indicatorMapper.selectList(
                            new LambdaQueryWrapper<IndicatorValueEntity>()
                                    .eq(IndicatorValueEntity::getReportId, TENANT_A_REPORT));
            assertThat(indicators).isEmpty();
        }
    }

    @Nested
    @DisplayName("随访任务隔离")
    class FollowupIsolation {
        @Test
        @DisplayName("租户A只能查到自己的随访")
        void tenantASeesOnlyOwnFollowups() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<FollowupTaskEntity> tasks = followupMapper.selectList(null);
            assertThat(tasks).isNotEmpty();
            assertThat(tasks).allSatisfy(t -> assertThat(t.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("租户B无法访问租户A的随访")
        void tenantBCannotAccessTenantAFollowup() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            List<FollowupTaskEntity> tasks =
                    followupMapper.selectList(
                            new LambdaQueryWrapper<FollowupTaskEntity>()
                                    .eq(FollowupTaskEntity::getPatientId, TENANT_A_PATIENT));
            assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("健康评估隔离")
    class AssessmentIsolation {
        @Test
        @DisplayName("租户A只能查到自己的评估")
        void tenantASeesOnlyOwnAssessments() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<HealthAssessmentEntity> assessments = assessmentMapper.selectList(null);
            assertThat(assessments).isNotEmpty();
            assertThat(assessments)
                    .allSatisfy(a -> assertThat(a.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("租户B无法访问租户A的评估")
        void tenantBCannotAccessTenantAAssessment() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            List<HealthAssessmentEntity> assessments =
                    assessmentMapper.selectList(
                            new LambdaQueryWrapper<HealthAssessmentEntity>()
                                    .eq(HealthAssessmentEntity::getPatientId, TENANT_A_PATIENT));
            assertThat(assessments).isEmpty();
        }
    }

    @Nested
    @DisplayName("审核任务隔离")
    class ReviewIsolation {
        @Test
        @DisplayName("租户A只能查到自己的审核")
        void tenantASeesOnlyOwnReviews() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<AssessmentReviewEntity> reviews = reviewMapper.selectList(null);
            assertThat(reviews).isNotEmpty();
            assertThat(reviews).allSatisfy(r -> assertThat(r.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("租户B无法访问租户A的审核")
        void tenantBCannotAccessTenantAReview() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            List<AssessmentReviewEntity> reviews =
                    reviewMapper.selectList(
                            new LambdaQueryWrapper<AssessmentReviewEntity>()
                                    .eq(AssessmentReviewEntity::getPatientId, TENANT_A_PATIENT));
            assertThat(reviews).isEmpty();
        }
    }

    @Nested
    @DisplayName("已发布健康报告隔离")
    class HealthReportIsolation {
        @Test
        @DisplayName("租户A只能查到自己的健康报告")
        void tenantASeesOnlyOwnHealthReports() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            List<HealthReportEntity> reports = healthReportMapper.selectList(null);
            assertThat(reports).isNotEmpty();
            assertThat(reports).allSatisfy(r -> assertThat(r.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("租户B无法访问租户A的健康报告")
        void tenantBCannotAccessTenantAHealthReport() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            List<HealthReportEntity> reports =
                    healthReportMapper.selectList(
                            new LambdaQueryWrapper<HealthReportEntity>()
                                    .eq(HealthReportEntity::getPatientId, TENANT_A_PATIENT));
            assertThat(reports).isEmpty();
        }
    }

    @Nested
    @DisplayName("写操作隔离")
    class WriteIsolation {
        @Test
        @DisplayName("租户A无法更新租户B的客户")
        void tenantACannotUpdateTenantBPatient() {
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            PatientEntity update = new PatientEntity();
            update.setId(TENANT_B_PATIENT);
            update.setName("被篡改的名称");
            int rows = patientMapper.updateById(update);
            assertThat(rows).isEqualTo(0);

            // 验证租户B数据未被修改
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            PatientEntity patient = patientMapper.selectById(TENANT_B_PATIENT);
            assertThat(patient).isNotNull();
            assertThat(patient.getName()).isEqualTo("租户B客户王五");
        }

        @Test
        @DisplayName("租户B无法删除租户A的报告")
        void tenantBCannotDeleteTenantAReport() {
            authenticateAsTenant(TENANT_B, 60001, "TENANT_ADMIN");
            int rows = labReportMapper.deleteById(TENANT_A_REPORT);
            assertThat(rows).isEqualTo(0);

            // 验证租户A数据未被删除
            authenticateAsTenant(TENANT_A, 10002, "TENANT_ADMIN");
            LabReportEntity report = labReportMapper.selectById(TENANT_A_REPORT);
            assertThat(report).isNotNull();
        }
    }

    @Nested
    @DisplayName("无上下文时拒绝访问")
    class NoContextRejection {
        @Test
        @DisplayName("无SecurityContext且无TenantContext时抛出异常")
        void throwsWhenNoContextAvailable() {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            assertThatThrownBy(() -> patientMapper.selectList(null))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage(
                            "租户上下文缺失：既无 TenantContext 也无 SecurityContext，"
                                    + "请检查是否在异步线程或定时任务中遗漏了租户设置");
        }
    }
}
