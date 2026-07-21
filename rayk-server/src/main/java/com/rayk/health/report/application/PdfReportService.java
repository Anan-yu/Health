package com.rayk.health.report.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.rayk.health.report.entity.HealthReportEntity;
import com.rayk.health.report.entity.HealthReportVersionEntity;
import com.rayk.health.report.mapper.HealthReportMapper;
import com.rayk.health.report.mapper.HealthReportVersionMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.storage.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
        this.dataScopeService = dataScopeService;
    }

    /** Generates a real PDF report through the AI document service and stores it in MinIO. */
    public String generateAndStore(
            HealthReportEntity report,
            HealthAssessmentEntity assessment,
            PatientEntity patient,
            long operatorId) {
        List<IndicatorValueEntity> indicators = indicatorValueMapper.selectList(
                new LambdaQueryWrapper<IndicatorValueEntity>()
                        .eq(IndicatorValueEntity::getReportId, assessment.getReportId())
                        .eq(IndicatorValueEntity::getDeleted, 0));

        List<AiDtos.ModelResult> modelResults = extractModelResults(assessment.getResultSnapshot());
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
                                modelResults));
        byte[] content = decodePdf(generated.pdfBase64());

        String objectPath = buildObjectPath(report.getTenantId(), report.getPatientId(), report.getId());
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
     * Generate a presigned download URL for the report file.
     */
    public String getDownloadUrl(long reportId) {
        HealthReportEntity report = healthReportMapper.selectById(reportId);
        if (report == null || !"PUBLISHED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND);
        }
        dataScopeService.requirePatient(report.getPatientId());

        HealthReportVersionEntity versionEntity = versionMapper.selectOne(
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

    private List<AiDtos.ModelResult> extractModelResults(String resultSnapshot) {
        try {
            JsonNode results = objectMapper.readTree(resultSnapshot).path("results");
            List<AiDtos.ModelResult> modelResults =
                    objectMapper.convertValue(
                            results, new TypeReference<List<AiDtos.ModelResult>>() {});
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
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.bucketReports()).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minioProperties.bucketReports()).build());
        }
    }
}
