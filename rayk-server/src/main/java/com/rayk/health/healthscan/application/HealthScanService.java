package com.rayk.health.healthscan.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.healthscan.config.HealthShotProperties;
import com.rayk.health.healthscan.entity.HealthScanTaskEntity;
import com.rayk.health.healthscan.integration.HealthShotSigner;
import com.rayk.health.healthscan.integration.HealthShotVendorClient;
import com.rayk.health.healthscan.integration.HealthShotVendorResult;
import com.rayk.health.healthscan.mapper.HealthScanTaskMapper;
import com.rayk.health.healthscan.vo.HealthScanResultVo;
import com.rayk.health.healthscan.vo.HealthScanSessionVo;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HealthScanService {
    private static final long MAX_VIDEO_BYTES = 220L * 1024L * 1024L;

    private final HealthShotProperties properties;
    private final HealthScanTaskMapper taskMapper;
    private final PatientMapper patientMapper;
    private final HealthShotVendorClient vendorClient;

    public HealthScanService(
            HealthShotProperties properties,
            HealthScanTaskMapper taskMapper,
            PatientMapper patientMapper,
            HealthShotVendorClient vendorClient) {
        this.properties = properties;
        this.taskMapper = taskMapper;
        this.patientMapper = patientMapper;
        this.vendorClient = vendorClient;
    }

    @Transactional
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public HealthScanSessionVo createSession() {
        requireConfigured();
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = requireCurrentPatient(current);
        LocalDateTime now = LocalDateTime.now();

        HealthScanTaskEntity task = new HealthScanTaskEntity();
        task.setTenantId(current.tenantId());
        task.setPatientId(patient.getId());
        task.setUserId(current.userId());
        task.setOutUserId(buildOutUserId(current, now));
        task.setStatus("CREATED");
        task.setEnvironment(valueOrDefault(properties.environment(), "UAT"));
        task.setPluginVersion(properties.pluginVersion());
        task.setStartedAt(now);
        task.setCreatedBy(current.userId());
        task.setCreatedAt(now);
        task.setUpdatedBy(current.userId());
        task.setUpdatedAt(now);
        task.setDeleted(0);
        task.setVersion(0);
        taskMapper.insert(task);

        long timestamp = System.currentTimeMillis();
        String sign =
                HealthShotSigner.sign(
                        Map.of(
                                "appId", properties.appId(),
                                "outUserId", task.getOutUserId(),
                                "timestamp", timestamp),
                        properties.key());
        return new HealthScanSessionVo(
                String.valueOf(task.getId()),
                properties.appId(),
                timestamp,
                task.getOutUserId(),
                sign,
                properties.pluginServerUrl(),
                valueOrDefault(properties.pluginProvider(), "wx9ad841f52fa9bd7b"),
                properties.pluginVersion());
    }

    @Transactional
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public HealthScanResultVo upload(long taskId, MultipartFile video) {
        CurrentPrincipal current = CurrentUser.require();
        HealthScanTaskEntity task = requireOwnedTask(taskId, current);
        if (!"CREATED".equals(task.getStatus()) && !"FAILED".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.HEALTH_SCAN_INVALID_STATUS);
        }
        validateVideo(video);

        try {
            byte[] bytes = video.getBytes();
            String digest = HealthShotSigner.md5(bytes);
            long timestamp = System.currentTimeMillis();
            task.setStatus("UPLOADING");
            task.setVideoDigest(digest);
            task.setUpdatedAt(LocalDateTime.now());
            task.setUpdatedBy(current.userId());
            taskMapper.updateById(task);

            HealthShotVendorResult result =
                    vendorClient.upload(
                            task.getOutUserId(),
                            timestamp,
                            digest,
                            bytes,
                            video.getOriginalFilename());
            applyVendorResult(task, result, current.userId());
            taskMapper.updateById(task);
            return toVo(task);
        } catch (IOException exception) {
            markFailed(task, current.userId(), "VIDEO_READ_FAILED", "无法读取检测视频");
            throw new BusinessException(ErrorCode.HEALTH_SCAN_SERVICE_UNAVAILABLE);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            markFailed(task, current.userId(), "VENDOR_UNAVAILABLE", "健康检测服务连接失败");
            throw new BusinessException(ErrorCode.HEALTH_SCAN_SERVICE_UNAVAILABLE);
        }
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public List<HealthScanResultVo> listMine() {
        CurrentPrincipal current = CurrentUser.require();
        return taskMapper
                .selectList(
                        new LambdaQueryWrapper<HealthScanTaskEntity>()
                                .eq(HealthScanTaskEntity::getTenantId, current.tenantId())
                                .eq(HealthScanTaskEntity::getUserId, current.userId())
                                .eq(HealthScanTaskEntity::getDeleted, 0)
                                .orderByDesc(HealthScanTaskEntity::getCreatedAt)
                                .last("LIMIT 20"))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public HealthScanResultVo get(long taskId) {
        return toVo(requireOwnedTask(taskId, CurrentUser.require()));
    }

    private void applyVendorResult(
            HealthScanTaskEntity task, HealthShotVendorResult result, long userId) {
        task.setVendorCode(result.code());
        task.setVendorMessage(result.message());
        task.setVendorDetectId(result.detectId());
        task.setHeartRate(result.heartRate());
        task.setHeartRateVariability(result.heartRateVariability());
        task.setOxygenSaturation(result.oxygenSaturation());
        task.setRespirationRate(result.respirationRate());
        task.setSystolicBloodPressure(result.systolicBloodPressure());
        task.setDiastolicBloodPressure(result.diastolicBloodPressure());
        task.setStressHrv(result.stressHrv());
        task.setQualityScore(result.qualityScore());
        task.setRawResultJson(result.rawJson());
        task.setUpdatedBy(userId);
        task.setUpdatedAt(LocalDateTime.now());
        if (result.failed()) {
            task.setStatus("FAILED");
        } else if (result.completed()) {
            task.setStatus("SUCCEEDED");
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setStatus("PROCESSING");
        }
    }

    private void markFailed(
            HealthScanTaskEntity task, long userId, String code, String message) {
        task.setStatus("FAILED");
        task.setVendorCode(code);
        task.setVendorMessage(message);
        task.setUpdatedBy(userId);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void validateVideo(MultipartFile video) {
        if (video == null
                || video.isEmpty()
                || video.getSize() > MAX_VIDEO_BYTES
                || (video.getContentType() != null
                        && !video.getContentType().startsWith("video/"))) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
    }

    private HealthScanTaskEntity requireOwnedTask(long taskId, CurrentPrincipal current) {
        HealthScanTaskEntity task =
                taskMapper.selectOne(
                        new LambdaQueryWrapper<HealthScanTaskEntity>()
                                .eq(HealthScanTaskEntity::getId, taskId)
                                .eq(HealthScanTaskEntity::getTenantId, current.tenantId())
                                .eq(HealthScanTaskEntity::getUserId, current.userId())
                                .eq(HealthScanTaskEntity::getDeleted, 0));
        if (task == null) {
            throw new BusinessException(ErrorCode.HEALTH_SCAN_NOT_FOUND);
        }
        return task;
    }

    private PatientEntity requireCurrentPatient(CurrentPrincipal current) {
        PatientEntity patient =
                patientMapper.selectOne(
                        new LambdaQueryWrapper<PatientEntity>()
                                .eq(PatientEntity::getTenantId, current.tenantId())
                                .eq(PatientEntity::getUserId, current.userId())
                                .eq(PatientEntity::getDeleted, 0)
                                .last("LIMIT 1"));
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        }
        return patient;
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new BusinessException(ErrorCode.HEALTH_SCAN_NOT_CONFIGURED);
        }
    }

    private String buildOutUserId(CurrentPrincipal current, LocalDateTime now) {
        return "zy-" + current.userId() + "-" + System.currentTimeMillis();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private HealthScanResultVo toVo(HealthScanTaskEntity task) {
        return new HealthScanResultVo(
                String.valueOf(task.getId()),
                task.getStatus(),
                statusLabel(task.getStatus()),
                task.getVendorDetectId(),
                task.getHeartRate(),
                task.getHeartRateVariability(),
                task.getOxygenSaturation(),
                task.getRespirationRate(),
                task.getSystolicBloodPressure(),
                task.getDiastolicBloodPressure(),
                task.getStressHrv(),
                task.getQualityScore(),
                "FAILED".equals(task.getStatus()) ? task.getVendorMessage() : null,
                task.getCreatedAt(),
                task.getCompletedAt());
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "CREATED" -> "待检测";
            case "UPLOADING" -> "上传中";
            case "PROCESSING" -> "分析中";
            case "SUCCEEDED" -> "已完成";
            case "FAILED" -> "检测失败";
            default -> "处理中";
        };
    }
}

