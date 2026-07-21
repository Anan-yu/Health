package com.rayk.health.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.assessment.mapper.HealthAssessmentMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.converter.PatientConverter;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.review.dto.ReviewEditRequest;
import com.rayk.health.review.entity.AssessmentReviewEntity;
import com.rayk.health.review.mapper.AssessmentReviewMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ReviewEditServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AssessmentReviewMapper reviewMapper;
    private HealthAssessmentMapper assessmentMapper;
    private ReviewEditService service;
    private HealthAssessmentEntity assessment;

    @BeforeEach
    void setUp() {
        reviewMapper = mock(AssessmentReviewMapper.class);
        assessmentMapper = mock(HealthAssessmentMapper.class);
        DataScopeService dataScopeService = mock(DataScopeService.class);
        PatientConverter patientConverter = mock(PatientConverter.class);
        service =
                new ReviewEditService(
                        reviewMapper,
                        assessmentMapper,
                        dataScopeService,
                        patientConverter,
                        objectMapper);

        AssessmentReviewEntity review = new AssessmentReviewEntity();
        review.setId(10L);
        review.setAssessmentId(20L);
        review.setPatientId(30L);
        review.setStatus("WAITING_REVIEW");
        when(reviewMapper.selectById(10L)).thenReturn(review);

        assessment = new HealthAssessmentEntity();
        assessment.setId(20L);
        assessment.setReportId(40L);
        assessment.setPatientId(30L);
        assessment.setModelVersion("TEST");
        assessment.setStatus("SUCCESS");
        assessment.setOverallRiskLevel("ATTENTION");
        assessment.setResultSnapshot(
                "{\"results\":[{\"modelCode\":\"MODEL_A\",\"riskLevel\":\"ATTENTION\","
                        + "\"evidence\":[\"old\"],\"recommendations\":[\"old\"]}]}");
        when(assessmentMapper.selectById(20L)).thenReturn(assessment);
        when(dataScopeService.requirePatient(30L)).thenReturn(new PatientEntity());

        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "test-jti",
                        "doctor",
                        10003L,
                        20001L,
                        List.of("DOCTOR"),
                        List.of("assessment:review"),
                        "DOCTOR");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsEvidenceAndRecommendationsAsJsonArraysAndRecomputesOverallRisk() throws Exception {
        service.editReview(
                10L,
                new ReviewEditRequest(
                        List.of(
                                new ReviewEditRequest.ReviewItemEdit(
                                        "MODEL_A",
                                        "LOW",
                                        List.of("verified evidence"),
                                        List.of("verified recommendation"))),
                        "reviewed"));

        JsonNode result = objectMapper.readTree(assessment.getResultSnapshot()).path("results").get(0);
        assertThat(result.path("evidence").isArray()).isTrue();
        assertThat(result.path("evidence").get(0).asText()).isEqualTo("verified evidence");
        assertThat(result.path("recommendations").isArray()).isTrue();
        assertThat(result.path("doctorEdited").asBoolean()).isTrue();
        assertThat(assessment.getOverallRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void rejectsUnknownModelCodeInsteadOfSilentlyIgnoringIt() {
        ReviewEditRequest request =
                new ReviewEditRequest(
                        List.of(
                                new ReviewEditRequest.ReviewItemEdit(
                                        "UNKNOWN", "LOW", List.of("evidence"), List.of("advice"))),
                        null);

        assertThatThrownBy(() -> service.editReview(10L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void preservesHighAsTheHighestOverallRisk() {
        service.editReview(
                10L,
                new ReviewEditRequest(
                        List.of(
                                new ReviewEditRequest.ReviewItemEdit(
                                        "MODEL_A",
                                        "HIGH",
                                        List.of("verified evidence"),
                                        List.of("urgent follow-up"))),
                        null));

        assertThat(assessment.getOverallRiskLevel()).isEqualTo("HIGH");
    }
}
