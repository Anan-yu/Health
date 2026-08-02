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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.LinkedHashMap;
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
            PatientEntity patient = requireCurrentPatient(current);
            return toVo(task, compareWithPeers(task, loadPeerScores(patient)));
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
        PatientEntity patient = requireCurrentPatient(current);
        List<HealthScanTaskEntity> tasks =
                taskMapper
                .selectList(
                        new LambdaQueryWrapper<HealthScanTaskEntity>()
                                .eq(HealthScanTaskEntity::getTenantId, current.tenantId())
                                .eq(HealthScanTaskEntity::getUserId, current.userId())
                                .eq(HealthScanTaskEntity::getDeleted, 0)
                                .orderByDesc(HealthScanTaskEntity::getCreatedAt)
                                .last("LIMIT 20"));
        List<Integer> peerScores = loadPeerScores(patient);
        return tasks
                .stream()
                .map(task -> toVo(task, compareWithPeers(task, peerScores)))
                .toList();
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public HealthScanResultVo get(long taskId) {
        CurrentPrincipal current = CurrentUser.require();
        HealthScanTaskEntity task = requireOwnedTask(taskId, current);
        PatientEntity patient = requireCurrentPatient(current);
        return toVo(task, compareWithPeers(task, loadPeerScores(patient)));
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

    private HealthScanResultVo toVo(
            HealthScanTaskEntity task, PeerComparison peerComparison) {
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
                peerComparison.healthScore(),
                peerComparison.percentile(),
                peerComparison.sampleSize(),
                peerComparison.estimated(),
                "FAILED".equals(task.getStatus()) ? task.getVendorMessage() : null,
                task.getCreatedAt(),
                task.getCompletedAt());
    }

    /**
     * 同龄对比使用平台内相同性别、年龄相差不超过 5 岁用户的最近一次成功检测。
     * 样本不足 5 人时返回明确标记的参考模型估算值，前端需向用户说明。
     */
    private List<Integer> loadPeerScores(PatientEntity currentPatient) {
        if (currentPatient.getBirthDate() == null) {
            return List.of();
        }
        List<HealthScanTaskEntity> candidates =
                taskMapper.selectList(
                        new LambdaQueryWrapper<HealthScanTaskEntity>()
                                .eq(HealthScanTaskEntity::getStatus, "SUCCEEDED")
                                .eq(HealthScanTaskEntity::getDeleted, 0)
                                .isNotNull(HealthScanTaskEntity::getPatientId)
                                .orderByDesc(HealthScanTaskEntity::getCompletedAt)
                                .last("LIMIT 5000"));
        Map<Long, HealthScanTaskEntity> latestByPatient = new LinkedHashMap<>();
        candidates.forEach(
                task -> latestByPatient.putIfAbsent(task.getPatientId(), task));
        if (latestByPatient.isEmpty()) {
            return List.of();
        }

        Map<Long, PatientEntity> patients = new LinkedHashMap<>();
        patientMapper.selectBatchIds(latestByPatient.keySet()).forEach(
                patient -> patients.put(patient.getId(), patient));
        int currentAge = ageOf(currentPatient.getBirthDate());
        String currentGender = normalizeGender(currentPatient.getGender());
        return latestByPatient.values().stream()
                .filter(task -> {
                    PatientEntity patient = patients.get(task.getPatientId());
                    if (patient == null || patient.getBirthDate() == null) {
                        return false;
                    }
                    if (!currentGender.isBlank()
                            && !currentGender.equals(normalizeGender(patient.getGender()))) {
                        return false;
                    }
                    return Math.abs(ageOf(patient.getBirthDate()) - currentAge) <= 5;
                })
                .map(this::calculateHealthScore)
                .toList();
    }

    private PeerComparison compareWithPeers(
            HealthScanTaskEntity task, List<Integer> peerScores) {
        int score = calculateHealthScore(task);
        if (peerScores.size() >= 5) {
            long lower = peerScores.stream().filter(value -> value < score).count();
            long equal = peerScores.stream().filter(value -> value == score).count();
            int percentile =
                    clamp(
                            (int)
                                    Math.round(
                                            (lower + equal * 0.5D)
                                                    * 100D
                                                    / peerScores.size()),
                            1,
                            99);
            return new PeerComparison(score, percentile, peerScores.size(), false);
        }
        // 参考模型只用于样本积累期，并通过 estimated 字段向前端明确标识。
        int estimatedPercentile =
                clamp((int) Math.round((score - 50D) * 82D / 45D), 5, 95);
        return new PeerComparison(score, estimatedPercentile, peerScores.size(), true);
    }

    private int calculateHealthScore(HealthScanTaskEntity task) {
        int penalty = 0;
        int available = 0;

        if (task.getHeartRate() != null) {
            available++;
            double value = task.getHeartRate().doubleValue();
            if (value < 60 || value > 100) {
                penalty += 8;
            }
        }
        if (task.getSystolicBloodPressure() != null
                && task.getDiastolicBloodPressure() != null) {
            available++;
            double systolic = task.getSystolicBloodPressure().doubleValue();
            double diastolic = task.getDiastolicBloodPressure().doubleValue();
            if (systolic >= 180 || diastolic >= 120) {
                penalty += 18;
            } else if (systolic >= 140
                    || diastolic >= 90
                    || systolic < 90
                    || diastolic < 60) {
                penalty += 8;
            }
        }
        if (task.getOxygenSaturation() != null) {
            available++;
            double value = task.getOxygenSaturation().doubleValue();
            penalty += value < 90 ? 18 : value < 95 ? 8 : 0;
        }
        if (task.getRespirationRate() != null) {
            available++;
            double value = task.getRespirationRate().doubleValue();
            if (value < 12 || value > 20) {
                penalty += 8;
            }
        }
        if (task.getHeartRateVariability() != null) {
            available++;
            if (task.getHeartRateVariability().compareTo(BigDecimal.valueOf(30)) < 0) {
                penalty += 8;
            }
        }
        if (task.getStressHrv() != null) {
            available++;
            if (task.getStressHrv().compareTo(BigDecimal.valueOf(0.4)) > 0) {
                penalty += 8;
            }
        }
        return available == 0 ? 0 : Math.max(45, 100 - penalty);
    }

    private int ageOf(LocalDate birthDate) {
        return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
    }

    private String normalizeGender(String gender) {
        return gender == null ? "" : gender.trim().toUpperCase();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PeerComparison(
            int healthScore, int percentile, int sampleSize, boolean estimated) {}

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
