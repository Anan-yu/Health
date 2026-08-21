package com.rayk.health.laboratory.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.laboratory.dto.CreateLabReportRequest;
import com.rayk.health.laboratory.entity.LabReportEntity;
import com.rayk.health.laboratory.entity.LabReportFileEntity;
import com.rayk.health.laboratory.mapper.LabReportFileMapper;
import com.rayk.health.laboratory.mapper.LabReportMapper;
import com.rayk.health.laboratory.vo.LabReportFileVo;
import com.rayk.health.laboratory.vo.LabReportUploadVo;
import com.rayk.health.laboratory.vo.LabReportVo;
import com.rayk.health.laboratory.vo.OcrTaskVo;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.storage.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LabReportFileService {
    private static final Logger logger = LoggerFactory.getLogger(LabReportFileService.class);

    private final MinioClient minioClient;
    private final MinioClient minioPublicClient;
    private final MinioProperties properties;
    private final WorkflowApplicationService workflowService;
    private final DataScopeService dataScopeService;
    private final LabReportFileMapper fileMapper;
    private final LabReportMapper reportMapper;
    private final OcrTaskService ocrTaskService;
    private final ApplicationEventPublisher eventPublisher;

    public LabReportFileService(
            MinioClient minioClient,
            @Qualifier("minioPublicClient") MinioClient minioPublicClient,
            MinioProperties properties,
            WorkflowApplicationService workflowService,
            DataScopeService dataScopeService,
            LabReportFileMapper fileMapper,
            LabReportMapper reportMapper,
            OcrTaskService ocrTaskService,
            ApplicationEventPublisher eventPublisher) {
        this.minioClient = minioClient;
        this.minioPublicClient = minioPublicClient;
        this.properties = properties;
        this.workflowService = workflowService;
        this.dataScopeService = dataScopeService;
        this.fileMapper = fileMapper;
        this.reportMapper = reportMapper;
        this.ocrTaskService = ocrTaskService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @PreAuthorize("hasAuthority('lab-report:manage') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    public LabReportUploadVo upload(
            long patientId, String reportName, LocalDate reportDate, MultipartFile file) {
        return upload(patientId, reportName, reportDate, file, true);
    }

    @Transactional
    @PreAuthorize("hasAuthority('lab-report:manage') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    public LabReportUploadVo upload(
            long patientId,
            String reportName,
            LocalDate reportDate,
            MultipartFile file,
            boolean startOcr) {
        ValidatedFile validated = validate(file);
        String normalizedReportName =
                reportName == null || reportName.isBlank()
                        ? validated.originalName()
                        : sanitizeReportName(reportName);
        LabReportVo report =
                workflowService.createLabReport(
                        new CreateLabReportRequest(
                                patientId,
                                normalizedReportName,
                                reportDate == null ? LocalDate.now() : reportDate,
                                "MINIO_UPLOAD"));
        long reportId = Long.parseLong(report.id());
        CurrentPrincipal current = CurrentUser.require();
        try {
            LabReportFileEntity entity = storeFile(reportId, patientId, validated, current);
            OcrTaskVo ocrTask = null;
            if (startOcr) {
                if (isImage(validated.mimeType())) {
                    publishImageReportSubmitted(reportId, current.tenantId());
                } else {
                    ocrTask = ocrTaskService.start(reportId, entity.getId());
                }
            }
            return new LabReportUploadVo(
                    workflowService.getLabReport(reportId), toVo(entity, true), ocrTask);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            logStorageFailure("upload", reportId, null, exception);
            markReportFailed(reportId, current.userId());
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    /** Stores an additional page on an existing report without starting OCR yet. */
    @Transactional
    @PreAuthorize("hasAuthority('lab-report:manage') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    public LabReportFileVo append(long reportId, MultipartFile file) {
        LabReportVo report = workflowService.getLabReport(reportId);
        ensureFilesCanBeAdded(report);
        ValidatedFile validated = validate(file);
        try {
            LabReportFileEntity entity =
                    storeFile(
                            reportId,
                            Long.parseLong(report.patientId()),
                            validated,
                            CurrentUser.require());
            return toVo(entity, false);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            logStorageFailure("append", reportId, null, exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    /** Starts OCR for PDF reports, or the qwen direct-read assessment for image-only reports. */
    @Transactional
    @PreAuthorize("hasAuthority('lab-report:manage') or (hasAuthority('self:lab-report') and principal.workbench == 'CUSTOMER')")
    public OcrTaskVo completeUpload(long reportId) {
        LabReportVo report = workflowService.getLabReport(reportId);
        ensureFilesCanBeAdded(report);
        LabReportFileEntity first =
                fileMapper.selectOne(
                        new LambdaQueryWrapper<LabReportFileEntity>()
                                .eq(LabReportFileEntity::getReportId, reportId)
                                .eq(LabReportFileEntity::getStatus, "STORED")
                                .eq(LabReportFileEntity::getDeleted, 0)
                                .orderByAsc(LabReportFileEntity::getCreatedAt)
                                .last("LIMIT 1"));
        if (first == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (isImageReport(reportId)) {
            publishImageReportSubmitted(reportId, CurrentUser.require().tenantId());
            return null;
        }
        return ocrTaskService.start(reportId, first.getId());
    }

    private void ensureFilesCanBeAdded(LabReportVo report) {
        if (report != null && "PUBLISHED".equals(report.status())) {
            throw new BusinessException(ErrorCode.LAB_REPORT_INVALID_STATUS);
        }
    }

    private boolean isImageReport(long reportId) {
        List<LabReportFileEntity> files =
                fileMapper.selectList(
                        new LambdaQueryWrapper<LabReportFileEntity>()
                                .eq(LabReportFileEntity::getReportId, reportId)
                                .eq(LabReportFileEntity::getStatus, "STORED")
                                .eq(LabReportFileEntity::getDeleted, 0));
        return !files.isEmpty()
                && files.stream().allMatch(file -> isImage(file.getMimeType()));
    }

    private boolean isImage(String mimeType) {
        return mimeType != null
                && mimeType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private void publishImageReportSubmitted(long reportId, long tenantId) {
        eventPublisher.publishEvent(new OcrTaskService.ImageReportSubmitted(reportId, tenantId));
    }

    private LabReportFileEntity storeFile(
            long reportId, long patientId, ValidatedFile validated, CurrentPrincipal current)
            throws Exception {
        String objectPath = objectPath(current.tenantId(), patientId, reportId, validated.extension());
        boolean objectStored = false;
        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucketReports())
                            .object(objectPath)
                            .contentType(validated.mimeType())
                            .stream(
                                    new ByteArrayInputStream(validated.bytes()),
                                    validated.bytes().length,
                                    -1)
                            .build());
            objectStored = true;
            LabReportFileEntity entity = new LabReportFileEntity();
            LocalDateTime now = LocalDateTime.now();
            entity.setTenantId(current.tenantId());
            entity.setReportId(reportId);
            entity.setBucketName(properties.bucketReports());
            entity.setObjectPath(objectPath);
            entity.setOriginalName(validated.originalName());
            entity.setMimeType(validated.mimeType());
            entity.setFileSize((long) validated.bytes().length);
            entity.setSha256(sha256(validated.bytes()));
            entity.setStatus("STORED");
            entity.setCreatedBy(current.userId());
            entity.setCreatedAt(now);
            entity.setUpdatedBy(current.userId());
            entity.setUpdatedAt(now);
            entity.setDeleted(0);
            entity.setVersion(0);
            fileMapper.insert(entity);
            return entity;
        } catch (Exception exception) {
            if (objectStored) {
                removeQuietly(objectPath);
            }
            throw exception;
        }
    }

    public List<LabReportFileVo> list(long reportId) {
        workflowService.getLabReport(reportId);
        return dataScopeService
                .readScoped(
                        () ->
                                fileMapper.selectList(
                                        new LambdaQueryWrapper<LabReportFileEntity>()
                                                .eq(LabReportFileEntity::getReportId, reportId)
                                                .eq(LabReportFileEntity::getDeleted, 0)
                                                .orderByDesc(LabReportFileEntity::getCreatedAt)))
                .stream()
                .map(entity -> toVo(entity, false))
                .toList();
    }

    public LabReportFileVo createDownloadUrl(long reportId, long fileId) {
        workflowService.getLabReport(reportId);
        LabReportFileEntity entity =
                dataScopeService.readScoped(() -> fileMapper.selectById(fileId));
        if (entity == null || entity.getReportId() != reportId || !"STORED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return toVo(entity, true);
    }

    /** Opens the protected file through the application gateway for physical mobile devices. */
    public DownloadedFile openContent(long reportId, long fileId) {
        workflowService.getLabReport(reportId);
        LabReportFileEntity entity =
                dataScopeService.readScoped(() -> fileMapper.selectById(fileId));
        if (entity == null || entity.getReportId() != reportId || !"STORED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            return new DownloadedFile(
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(entity.getBucketName())
                                    .object(entity.getObjectPath())
                                    .build()),
                    entity.getOriginalName(),
                    entity.getMimeType());
        } catch (Exception exception) {
            logStorageFailure("open_content", reportId, entity.getId(), exception);
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    public record DownloadedFile(InputStream inputStream, String originalName, String mimeType) {}

    private LabReportFileVo toVo(LabReportFileEntity entity, boolean includeDownloadUrl) {
        String url = null;
        LocalDateTime expiresAt = null;
        if (includeDownloadUrl) {
            try {
                url =
                        minioPublicClient.getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(entity.getBucketName())
                                        .object(entity.getObjectPath())
                                        .expiry(properties.presignExpirySeconds())
                                        .build());
                expiresAt = LocalDateTime.now().plusSeconds(properties.presignExpirySeconds());
            } catch (Exception exception) {
                logStorageFailure("presign", entity.getReportId(), entity.getId(), exception);
                throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
            }
        }
        return new LabReportFileVo(
                String.valueOf(entity.getId()),
                String.valueOf(entity.getReportId()),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getSha256(),
                entity.getStatus(),
                url,
                expiresAt,
                entity.getCreatedAt());
    }

    private ValidatedFile validate(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException(ErrorCode.FILE_INVALID);
            }
            String originalName = sanitizeName(file.getOriginalFilename());
            extension(originalName);
            byte[] bytes = file.getBytes();
            DetectedFileType detected = detectFileType(bytes);
            return new ValidatedFile(originalName, detected.extension(), detected.mimeType(), bytes);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
    }

    private DetectedFileType detectFileType(byte[] bytes) {
        if (startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            return new DetectedFileType("pdf", "application/pdf");
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return new DetectedFileType("jpg", "image/jpeg");
        }
        if (startsWith(
                bytes,
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {
            return new DetectedFileType("png", "image/png");
        }
        throw new BusinessException(ErrorCode.FILE_INVALID);
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeName(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        String normalized = value.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (name.isBlank() || name.length() > 255 || name.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        return name;
    }

    private String extension(String originalName) {
        int separator = originalName.lastIndexOf('.');
        if (separator < 1 || separator == originalName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        String extension = originalName.substring(separator + 1).toLowerCase(Locale.ROOT);
        if (!List.of("pdf", "jpg", "jpeg", "png").contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        return extension;
    }

    private String sanitizeReportName(String value) {
        String name = value.trim();
        if (name.isBlank() || name.length() > 100 || name.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        return name;
    }

    private String objectPath(long tenantId, long patientId, long reportId, String extension) {
        return "%d/%d/%d/%s.%s"
                .formatted(
                        tenantId,
                        patientId,
                        reportId,
                        UUID.randomUUID().toString().replace("-", ""),
                        extension);
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private void ensureBucket() throws Exception {
        boolean exists =
                minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(properties.bucketReports()).build());
        if (!exists) {
            try {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.bucketReports()).build());
            } catch (ErrorResponseException exception) {
                // Two first uploads can observe the bucket as absent at the same time.
                // The losing request can safely continue when the other request created it.
                String code = exception.errorResponse().code();
                if (!"BucketAlreadyOwnedByYou".equals(code)
                        && !"BucketAlreadyExists".equals(code)) {
                    throw exception;
                }
            }
        }
    }

    private void removeQuietly(String objectPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucketReports())
                            .object(objectPath)
                            .build());
        } catch (Exception ignored) {
            // A later storage reconciliation job can clean up orphaned objects.
        }
    }

    private void markReportFailed(long reportId, long userId) {
        LabReportEntity report = reportMapper.selectById(reportId);
        if (report != null) {
            report.setStatus("FAILED");
            report.setFailureReason("文件存储失败");
            report.setUpdatedBy(userId);
            report.setUpdatedAt(LocalDateTime.now());
            reportMapper.updateById(report);
        }
    }

    private void logStorageFailure(
            String operation, long reportId, Long fileId, Exception exception) {
        logger.error(
                "Lab report file operation failed operation={} reportId={} fileId={} "
                        + "errorType={} minioCode={}",
                operation,
                reportId,
                fileId,
                exception.getClass().getSimpleName(),
                minioErrorCode(exception));
    }

    private String minioErrorCode(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ErrorResponseException error) {
                return error.errorResponse().code();
            }
            current = current.getCause();
        }
        return "none";
    }

    private record ValidatedFile(
            String originalName, String extension, String mimeType, byte[] bytes) {}

    private record DetectedFileType(String extension, String mimeType) {}
}
