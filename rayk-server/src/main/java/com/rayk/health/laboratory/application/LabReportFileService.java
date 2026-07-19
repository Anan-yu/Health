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
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.storage.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LabReportFileService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private final MinioClient minioClient;
    private final MinioClient minioPublicClient;
    private final MinioProperties properties;
    private final WorkflowApplicationService workflowService;
    private final LabReportFileMapper fileMapper;
    private final LabReportMapper reportMapper;

    public LabReportFileService(
            MinioClient minioClient,
            @Qualifier("minioPublicClient") MinioClient minioPublicClient,
            MinioProperties properties,
            WorkflowApplicationService workflowService,
            LabReportFileMapper fileMapper,
            LabReportMapper reportMapper) {
        this.minioClient = minioClient;
        this.minioPublicClient = minioPublicClient;
        this.properties = properties;
        this.workflowService = workflowService;
        this.fileMapper = fileMapper;
        this.reportMapper = reportMapper;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('lab-report:manage', 'self:lab-report')")
    public LabReportUploadVo upload(
            long patientId, String reportName, LocalDate reportDate, MultipartFile file) {
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
            return new LabReportUploadVo(report, toVo(entity, true));
        } catch (BusinessException exception) {
            if (objectStored) {
                removeQuietly(objectPath);
            }
            throw exception;
        } catch (Exception exception) {
            if (objectStored) {
                removeQuietly(objectPath);
            }
            markReportFailed(reportId, current.userId());
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    public List<LabReportFileVo> list(long reportId) {
        workflowService.getLabReport(reportId);
        return fileMapper
                .selectList(
                        new LambdaQueryWrapper<LabReportFileEntity>()
                                .eq(LabReportFileEntity::getReportId, reportId)
                                .eq(LabReportFileEntity::getDeleted, 0)
                                .orderByDesc(LabReportFileEntity::getCreatedAt))
                .stream()
                .map(entity -> toVo(entity, false))
                .toList();
    }

    public LabReportFileVo createDownloadUrl(long reportId, long fileId) {
        workflowService.getLabReport(reportId);
        LabReportFileEntity entity = fileMapper.selectById(fileId);
        if (entity == null || entity.getReportId() != reportId || !"STORED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return toVo(entity, true);
    }

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
            if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(ErrorCode.FILE_INVALID);
            }
            String originalName = sanitizeName(file.getOriginalFilename());
            String extension = extension(originalName);
            byte[] bytes = file.getBytes();
            String mimeType = detectMime(extension, bytes);
            return new ValidatedFile(originalName, extension, mimeType, bytes);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
    }

    private String detectMime(String extension, byte[] bytes) {
        boolean valid =
                switch (extension) {
                    case "pdf" -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
                    case "jpg", "jpeg" ->
                            bytes.length >= 3
                                    && (bytes[0] & 0xff) == 0xff
                                    && (bytes[1] & 0xff) == 0xd8
                                    && (bytes[2] & 0xff) == 0xff;
                    case "png" ->
                            startsWith(
                                    bytes,
                                    new byte[] {
                                        (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
                                    });
                    default -> false;
                };
        if (!valid) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            default -> "image/jpeg";
        };
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
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(properties.bucketReports()).build());
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

    private record ValidatedFile(
            String originalName, String extension, String mimeType, byte[] bytes) {}
}
