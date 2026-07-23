package com.rayk.health.report.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.assessment.mapper.HealthAssessmentMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.indicator.entity.IndicatorValueEntity;
import com.rayk.health.indicator.mapper.IndicatorValueMapper;
import com.rayk.health.integration.ai.AiDtos;
import com.rayk.health.integration.ai.AiServiceClient;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.report.entity.HealthReportEntity;
import com.rayk.health.report.entity.HealthReportVersionEntity;
import com.rayk.health.report.mapper.HealthReportMapper;
import com.rayk.health.report.mapper.HealthReportVersionMapper;
import com.rayk.health.storage.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PdfReportService {
    private final MinioClient minioClient;
    private final MinioClient minioPublicClient;
    private final MinioProperties minioProperties;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final HealthReportMapper healthReportMapper;
    private final HealthReportVersionMapper versionMapper;
    private final HealthAssessmentMapper assessmentMapper;
    private final IndicatorValueMapper indicatorValueMapper;
    private final PatientMapper patientMapper;
    private final DataScopeService dataScopeService;

    public PdfReportService(
            MinioClient minioClient,
            @Qualifier("minioPublicClient") MinioClient minioPublicClient,
            MinioProperties minioProperties,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper,
            HealthReportMapper healthReportMapper,
            HealthReportVersionMapper versionMapper,
            HealthAssessmentMapper assessmentMapper,
            IndicatorValueMapper indicatorValueMapper,
            PatientMapper patientMapper,
            DataScopeService dataScopeService) {
        this.minioClient = minioClient;
        this.minioPublicClient = minioPublicClient;
        this.minioProperties = minioProperties;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        this.healthReportMapper = healthReportMapper;
        this.versionMapper = versionMapper;
        this.assessmentMapper = assessmentMapper;
        this.indicatorValueMapper = indicatorValueMapper;
        this.patientMapper = patientMapper;
        this.dataScopeService = dataScopeService;
    }

    /** Generates a real PDF report through the AI document service and stores it in MinIO. */
    public String generateAndStore(
            HealthReportEntity report,
            HealthAssessmentEntity assessment,
            PatientEntity patient,
            long operatorId) {
        GeneratedPdf generated = renderReport(report, assessment, patient);
        byte[] content = generated.content();

    String objectPath =
        buildObjectPath(report.getTenantId(), report.getPatientId(), report.getId());
        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucketReports())
                            .object(objectPath)
                            .contentType("application/pdf")
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }

        // Save version record
        HealthReportVersionEntity versionEntity = new HealthReportVersionEntity();
        versionEntity.setTenantId(report.getTenantId());
        versionEntity.setHealthReportId(report.getId());
        versionEntity.setVersionNo(1);
        versionEntity.setContentSnapshot(
                "{\"format\":\"PDF\",\"title\":%s}"
                        .formatted(objectMapper.valueToTree(generated.title()).toString()));
        versionEntity.setObjectPath(objectPath);
        versionEntity.setCreatedBy(operatorId);
        versionEntity.setCreatedAt(LocalDateTime.now());
        versionEntity.setUpdatedBy(operatorId);
        versionEntity.setUpdatedAt(LocalDateTime.now());
        versionEntity.setDeleted(0);
        versionEntity.setVersion(0);
        versionMapper.insert(versionEntity);

        return objectPath;
    }

    /**
     * Repairs a missing artifact without changing or overwriting any existing report version.
     * The report row is locked so concurrent repair requests serialize per report.
     */
    @Transactional
    public ArtifactRecoveryResult recoverMissingPublishedArtifact(long reportId, long operatorId) {
        HealthReportEntity report = healthReportMapper.selectByIdForUpdate(reportId);
        if (report == null || !"PUBLISHED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }

        List<HealthReportVersionEntity> versions =
                versionMapper.selectList(
                        new LambdaQueryWrapper<HealthReportVersionEntity>()
                                .eq(HealthReportVersionEntity::getHealthReportId, reportId)
                                .eq(HealthReportVersionEntity::getDeleted, 0)
                                .orderByDesc(HealthReportVersionEntity::getVersionNo));
        HealthReportVersionEntity latest = versions.isEmpty() ? null : versions.getFirst();
        if (latest != null
                && latest.getObjectPath() != null
                && objectExists(latest.getObjectPath())) {
            return new ArtifactRecoveryResult(
                    reportId, "SKIPPED", latest.getVersionNo(), latest.getObjectPath());
        }

        int nextVersion = latest == null ? 1 : latest.getVersionNo() + 1;
        String objectPath =
                buildRecoveryObjectPath(
                        report.getTenantId(), report.getPatientId(), report.getId(), nextVersion);
        if (objectExists(objectPath)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }

        HealthAssessmentEntity assessment = assessmentMapper.selectById(report.getAssessmentId());
        PatientEntity patient = patientMapper.selectById(report.getPatientId());
        if (assessment == null || patient == null) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        GeneratedPdf generated = renderReport(report, assessment, patient);
        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucketReports())
                            .object(objectPath)
                            .contentType("application/pdf")
                            .stream(
                                    new ByteArrayInputStream(generated.content()),
                                    generated.content().length,
                                    -1)
                            .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }

        HealthReportVersionEntity versionEntity = new HealthReportVersionEntity();
        versionEntity.setTenantId(report.getTenantId());
        versionEntity.setHealthReportId(report.getId());
        versionEntity.setVersionNo(nextVersion);
        versionEntity.setContentSnapshot(
                "{\"format\":\"PDF\",\"title\":%s,\"recovered\":true}"
                        .formatted(objectMapper.valueToTree(generated.title()).toString()));
        versionEntity.setObjectPath(objectPath);
        versionEntity.setCreatedBy(operatorId);
        versionEntity.setCreatedAt(LocalDateTime.now());
        versionEntity.setUpdatedBy(operatorId);
        versionEntity.setUpdatedAt(LocalDateTime.now());
        versionEntity.setDeleted(0);
        versionEntity.setVersion(0);
        try {
            versionMapper.insert(versionEntity);
        } catch (RuntimeException exception) {
            removeNewObjectQuietly(objectPath);
            throw exception;
        }
        return new ArtifactRecoveryResult(reportId, "RECOVERED", nextVersion, objectPath);
    }

  /** Generate a presigned download URL for the report file. */
    public String getDownloadUrl(long reportId) {
        HealthReportEntity report = healthReportMapper.selectById(reportId);
        if (report == null || !"PUBLISHED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(report.getPatientId());

    HealthReportVersionEntity versionEntity =
        versionMapper.selectOne(
                new LambdaQueryWrapper<HealthReportVersionEntity>()
                        .eq(HealthReportVersionEntity::getHealthReportId, reportId)
                        .eq(HealthReportVersionEntity::getDeleted, 0)
                        .orderByDesc(HealthReportVersionEntity::getVersionNo)
                        .last("LIMIT 1"));
        if (versionEntity == null || versionEntity.getObjectPath() == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            return minioPublicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.bucketReports())
                            .object(versionEntity.getObjectPath())
                            .expiry(minioProperties.presignExpirySeconds())
                            .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    private String buildObjectPath(long tenantId, long patientId, long reportId) {
        return "%d/%d/%d/report.pdf".formatted(tenantId, patientId, reportId);
    }

    /** Opens the latest PDF through the authenticated application gateway for mobile clients. */
    public DownloadedPdf openDownload(long reportId) {
        HealthReportEntity report = healthReportMapper.selectById(reportId);
        if (report == null || !"PUBLISHED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(report.getPatientId());
        HealthReportVersionEntity versionEntity =
                versionMapper.selectOne(
                        new LambdaQueryWrapper<HealthReportVersionEntity>()
                                .eq(HealthReportVersionEntity::getHealthReportId, reportId)
                                .eq(HealthReportVersionEntity::getDeleted, 0)
                                .orderByDesc(HealthReportVersionEntity::getVersionNo)
                                .last("LIMIT 1"));
        if (versionEntity == null || versionEntity.getObjectPath() == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            return new DownloadedPdf(
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioProperties.bucketReports())
                                    .object(versionEntity.getObjectPath())
                                    .build()),
                    report.getTitle() + ".pdf");
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    public record DownloadedPdf(InputStream inputStream, String filename) {}

    private String buildRecoveryObjectPath(
            long tenantId, long patientId, long reportId, int versionNo) {
        return "%d/%d/%d/report-v%02d.pdf"
                .formatted(tenantId, patientId, reportId, versionNo);
    }

    private GeneratedPdf renderReport(
            HealthReportEntity report,
            HealthAssessmentEntity assessment,
            PatientEntity patient) {
        List<IndicatorValueEntity> indicators =
                indicatorValueMapper.selectList(
                        new LambdaQueryWrapper<IndicatorValueEntity>()
                                .eq(IndicatorValueEntity::getReportId, assessment.getReportId())
                                .eq(IndicatorValueEntity::getDeleted, 0));
        List<AiDtos.ModelResult> modelResults = extractModelResults(assessment.getResultSnapshot());
        AiDtos.ComprehensiveInterpretation interpretation =
                extractInterpretation(assessment.getResultSnapshot());
        AiDtos.ReportGenerateData generated =
                aiServiceClient.generateReport(
                        new AiDtos.ReportGenerateRequest(
                                String.valueOf(assessment.getId()),
                                patient.getName(),
                                report.getReportNo(),
                                report.getPublishedAt() == null
                                        ? null
                                        : report.getPublishedAt().toLocalDate().toString(),
                                report.getDoctorOpinion(),
                                indicators.stream()
                                        .map(
                                                item ->
                                                        new AiDtos.Indicator(
                                                                item.getIndicatorCode(),
                                                                item.getIndicatorName(),
                                                                item.getValue(),
                                                                item.getUnit(),
                                                                item.getReferenceLow(),
                                                                item.getReferenceHigh()))
                                        .toList(),
                                modelResults,
                                interpretation,
                                extractPatientContext(assessment.getResultSnapshot())));
        return new GeneratedPdf(generated.title(), decodePdf(generated.pdfBase64()));
    }

    private boolean objectExists(String objectPath) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.bucketReports())
                            .object(objectPath)
                            .build());
            return true;
        } catch (ErrorResponseException exception) {
            String code = exception.errorResponse().code();
            if ("NoSuchKey".equals(code)
                    || "NoSuchObject".equals(code)
                    || "NotFound".equals(code)) {
                return false;
            }
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    private void removeNewObjectQuietly(String objectPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucketReports())
                            .object(objectPath)
                            .build());
        } catch (Exception ignored) {
            // The database transaction still rolls back; an orphan is safer than deleting old data.
        }
    }

    public record ArtifactRecoveryResult(
            long reportId, String status, int versionNo, String objectPath) {}

    private record GeneratedPdf(String title, byte[] content) {}

    private AiDtos.PatientContext extractPatientContext(String resultSnapshot) {
        try {
            JsonNode node = objectMapper.readTree(resultSnapshot).path("patientContext");
            return node.isMissingNode() || node.isNull()
                    ? new AiDtos.PatientContext("UNKNOWN", null)
                    : objectMapper.convertValue(node, AiDtos.PatientContext.class);
        } catch (Exception exception) {
            return new AiDtos.PatientContext("UNKNOWN", null);
        }
    }

    private List<AiDtos.ModelResult> extractModelResults(String resultSnapshot) {
        try {
            JsonNode results = objectMapper.readTree(resultSnapshot).path("results");
            List<AiDtos.ModelResult> modelResults = new ArrayList<>();
            for (JsonNode node : results) {
                AiDtos.ModelResult item = objectMapper.convertValue(node, AiDtos.ModelResult.class);
                modelResults.add(
                        new AiDtos.ModelResult(
                                item.modelCode(),
                                item.modelName(),
                                node.hasNonNull("modelVersion")
                                        ? item.modelVersion()
                                        : "3.0.0",
                                node.hasNonNull("status")
                                        ? item.status()
                                        : (item.score() == null
                                                ? "INSUFFICIENT_DATA"
                                                : "EVALUATED"),
                                item.score(),
                                item.riskLevel(),
                                node.hasNonNull("dataCompleteness")
                                        ? item.dataCompleteness()
                                        : 100,
                                node.hasNonNull("confidence") ? item.confidence() : "MEDIUM",
                                item.evidence() == null ? List.of() : item.evidence(),
                                item.supportingIndicators() == null
                                        ? List.of()
                                        : item.supportingIndicators(),
                                item.missingIndicators() == null
                                        ? List.of()
                                        : item.missingIndicators(),
                                item.recommendations() == null
                                        ? List.of()
                                        : item.recommendations()));
            }
            if (modelResults == null || modelResults.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return modelResults;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

  private AiDtos.ComprehensiveInterpretation extractInterpretation(String resultSnapshot) {
    try {
      JsonNode interpretation = objectMapper.readTree(resultSnapshot).path("interpretation");
      if (interpretation.isMissingNode() || interpretation.isNull()) {
        return null;
      }
      return objectMapper.convertValue(interpretation, AiDtos.ComprehensiveInterpretation.class);
    } catch (Exception ignored) {
      return null;
    }
  }

    private byte[] decodePdf(String encodedPdf) {
        try {
            byte[] content = Base64.getDecoder().decode(encodedPdf);
            if (content.length < 5
                    || content[0] != '%'
                    || content[1] != 'P'
                    || content[2] != 'D'
                    || content[3] != 'F'
                    || content[4] != '-') {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return content;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private void ensureBucket() throws Exception {
    boolean exists =
        minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.bucketReports()).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minioProperties.bucketReports()).build());
        }
    }
}
