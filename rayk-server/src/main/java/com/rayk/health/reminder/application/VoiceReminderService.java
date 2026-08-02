package com.rayk.health.reminder.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.reminder.config.TencentTtsProperties;
import com.rayk.health.reminder.dto.UpdateVoiceReminderSettingRequest;
import com.rayk.health.reminder.entity.VoiceReminderAudioEntity;
import com.rayk.health.reminder.entity.VoiceReminderSettingEntity;
import com.rayk.health.reminder.integration.TencentTtsClient;
import com.rayk.health.reminder.mapper.VoiceReminderAudioMapper;
import com.rayk.health.reminder.mapper.VoiceReminderSettingMapper;
import com.rayk.health.reminder.vo.VoiceReminderPreviewVo;
import com.rayk.health.reminder.vo.VoiceReminderSettingVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.storage.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoiceReminderService {
    private static final LocalTime DEFAULT_MEAL_TIME = LocalTime.of(11, 30);
    private static final LocalTime DEFAULT_SLEEP_TIME = LocalTime.of(21, 30);
    private static final String TIMEZONE = "Asia/Shanghai";

    private final VoiceReminderSettingMapper settingMapper;
    private final VoiceReminderAudioMapper audioMapper;
    private final PatientMapper patientMapper;
    private final VoiceReminderTextFactory textFactory;
    private final TencentTtsClient ttsClient;
    private final TencentTtsProperties ttsProperties;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public VoiceReminderService(
            VoiceReminderSettingMapper settingMapper,
            VoiceReminderAudioMapper audioMapper,
            PatientMapper patientMapper,
            VoiceReminderTextFactory textFactory,
            TencentTtsClient ttsClient,
            TencentTtsProperties ttsProperties,
            MinioClient minioClient,
            MinioProperties minioProperties) {
        this.settingMapper = settingMapper;
        this.audioMapper = audioMapper;
        this.patientMapper = patientMapper;
        this.textFactory = textFactory;
        this.ttsClient = ttsClient;
        this.ttsProperties = ttsProperties;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public VoiceReminderSettingVo getSetting() {
        CurrentPrincipal current = CurrentUser.require();
        VoiceReminderSettingEntity setting = findSetting(current);
        return toVo(setting, patient(current));
    }

    @Transactional
    public VoiceReminderSettingVo updateSetting(UpdateVoiceReminderSettingRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        VoiceReminderSettingEntity setting = findSetting(current);
        LocalDateTime now = LocalDateTime.now();
        if (setting == null) {
            setting = new VoiceReminderSettingEntity();
            setting.setTenantId(current.tenantId());
            setting.setUserId(current.userId());
            setting.setCreatedBy(current.userId());
            setting.setCreatedAt(now);
            setting.setDeleted(0);
            setting.setVersion(0);
        }
        setting.setMealEnabled(request.mealEnabled());
        setting.setMealTime(request.mealTime());
        setting.setSleepEnabled(request.sleepEnabled());
        setting.setSleepTime(request.sleepTime());
        setting.setTimezone(TIMEZONE);
        setting.setUpdatedBy(current.userId());
        setting.setUpdatedAt(now);
        if (setting.getId() == null) {
            settingMapper.insert(setting);
        } else {
            settingMapper.updateById(setting);
        }
        return toVo(setting, patient(current));
    }

    @Transactional
    public VoiceReminderPreviewVo preview(String requestedType) {
        CurrentPrincipal current = CurrentUser.require();
        PatientEntity patient = patient(current);
        String type = normalizeType(requestedType);
        VoiceChoice voice = chooseVoice(patient.getGender());
        String text = textFactory.create(type, patient.getName());
        byte[] audio = ttsClient.synthesize(text, voice.type());
        String objectPath =
                "voice-reminders/%d/%d/%s.mp3"
                        .formatted(current.tenantId(), current.userId(), UUID.randomUUID());
        store(objectPath, audio);

        VoiceReminderAudioEntity entity = new VoiceReminderAudioEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(current.tenantId());
        entity.setUserId(current.userId());
        entity.setReminderType(type);
        entity.setTextContent(text);
        entity.setVoiceType(voice.type());
        entity.setVoiceName(voice.name());
        entity.setObjectPath(objectPath);
        entity.setCreatedBy(current.userId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(current.userId());
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        entity.setVersion(0);
        audioMapper.insert(entity);
        return new VoiceReminderPreviewVo(
                entity.getId(),
                type,
                text,
                voice.name(),
                "/api/v1/me/voice-reminders/audio/%d/content".formatted(entity.getId()));
    }

    public DownloadedAudio openAudio(long id) {
        CurrentPrincipal current = CurrentUser.require();
        VoiceReminderAudioEntity audio =
                audioMapper.selectOne(
                        new LambdaQueryWrapper<VoiceReminderAudioEntity>()
                                .eq(VoiceReminderAudioEntity::getId, id)
                                .eq(VoiceReminderAudioEntity::getTenantId, current.tenantId())
                                .eq(VoiceReminderAudioEntity::getUserId, current.userId())
                                .eq(VoiceReminderAudioEntity::getDeleted, 0));
        if (audio == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            InputStream stream =
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioProperties.bucketReports())
                                    .object(audio.getObjectPath())
                                    .build());
            return new DownloadedAudio(stream, "voice-reminder-%d.mp3".formatted(id));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
        }
    }

    private VoiceReminderSettingEntity findSetting(CurrentPrincipal current) {
        return settingMapper.selectOne(
                new LambdaQueryWrapper<VoiceReminderSettingEntity>()
                        .eq(VoiceReminderSettingEntity::getTenantId, current.tenantId())
                        .eq(VoiceReminderSettingEntity::getUserId, current.userId())
                        .eq(VoiceReminderSettingEntity::getDeleted, 0)
                        .last("LIMIT 1"));
    }

    private PatientEntity patient(CurrentPrincipal current) {
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

    private VoiceReminderSettingVo toVo(
            VoiceReminderSettingEntity setting, PatientEntity patient) {
        boolean mealEnabled = setting == null || Boolean.TRUE.equals(setting.getMealEnabled());
        LocalTime mealTime = setting == null ? DEFAULT_MEAL_TIME : setting.getMealTime();
        boolean sleepEnabled = setting == null || Boolean.TRUE.equals(setting.getSleepEnabled());
        LocalTime sleepTime = setting == null ? DEFAULT_SLEEP_TIME : setting.getSleepTime();
        String timezone = setting == null ? TIMEZONE : setting.getTimezone();
        VoiceChoice voice = chooseVoice(patient.getGender());
        return new VoiceReminderSettingVo(
                mealEnabled,
                mealTime,
                sleepEnabled,
                sleepTime,
                timezone,
                voice.description(),
                ttsClient.available());
    }

    private VoiceChoice chooseVoice(String gender) {
        String normalized = gender == null ? "" : gender.trim().toUpperCase(Locale.ROOT);
        if ("FEMALE".equals(normalized) || "女".equals(gender)) {
            return new VoiceChoice(ttsProperties.maleVoiceType(), "温暖男声", "将使用温暖男声提醒");
        }
        return new VoiceChoice(ttsProperties.femaleVoiceType(), "温柔女声", "将使用温柔女声提醒");
    }

    private String normalizeType(String type) {
        return "SLEEP".equalsIgnoreCase(type) ? "SLEEP" : "MEAL";
    }

    private void store(String objectPath, byte[] audio) {
        try {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucketReports())
                            .object(objectPath)
                            .contentType("audio/mpeg")
                            .stream(new ByteArrayInputStream(audio), audio.length, -1)
                            .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_UNAVAILABLE);
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

    private record VoiceChoice(int type, String name, String description) {}

    public record DownloadedAudio(InputStream inputStream, String filename) {}
}
