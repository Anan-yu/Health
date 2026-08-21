package com.rayk.health.platform.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.platform.entity.AiModelRuntimeConfigEntity;
import com.rayk.health.platform.mapper.AiModelRuntimeConfigMapper;
import com.rayk.health.platform.vo.AiModelRuntimeConfigVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiModelRuntimeConfigService {
    public record RuntimeSelection(String modelCode, boolean thinkingEnabled) {}

    private final AiModelRuntimeConfigMapper mapper;

    public AiModelRuntimeConfigService(AiModelRuntimeConfigMapper mapper) {
        this.mapper = mapper;
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<AiModelRuntimeConfigVo> listModels() {
        return mapper.selectList(
                        new LambdaQueryWrapper<AiModelRuntimeConfigEntity>()
                                .eq(AiModelRuntimeConfigEntity::getStatus, "ACTIVE")
                                .orderByAsc(AiModelRuntimeConfigEntity::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    /** Returns the selected provider model for an assessment request. */
    public RuntimeSelection currentSelection() {
        AiModelRuntimeConfigEntity selected =
                mapper.selectOne(
                        new LambdaQueryWrapper<AiModelRuntimeConfigEntity>()
                                .eq(AiModelRuntimeConfigEntity::getStatus, "ACTIVE")
                                .eq(AiModelRuntimeConfigEntity::getSelected, 1)
                                .last("LIMIT 1"));
        return selected == null
                ? new RuntimeSelection("deepseek-v4-flash", false)
                : new RuntimeSelection(
                        selected.getModelCode(),
                        selected.getThinkingEnabled() != null && selected.getThinkingEnabled() == 1);
    }

    public String currentModelCode() {
        return currentSelection().modelCode();
    }

    public boolean currentThinkingEnabled() {
        return currentSelection().thinkingEnabled();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Transactional
    public AiModelRuntimeConfigVo switchModel(String modelCode) {
        CurrentPrincipal current = CurrentUser.require();
        AiModelRuntimeConfigEntity target =
                mapper.selectOne(
                        new LambdaQueryWrapper<AiModelRuntimeConfigEntity>()
                                .eq(AiModelRuntimeConfigEntity::getModelCode, modelCode)
                                .eq(AiModelRuntimeConfigEntity::getStatus, "ACTIVE")
                                .last("LIMIT 1"));
        if (target == null) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        mapper.update(
                null,
                new LambdaUpdateWrapper<AiModelRuntimeConfigEntity>()
                        .eq(AiModelRuntimeConfigEntity::getStatus, "ACTIVE")
                        .set(AiModelRuntimeConfigEntity::getSelected, 0)
                        .set(AiModelRuntimeConfigEntity::getUpdatedBy, current.userId())
                        .set(AiModelRuntimeConfigEntity::getUpdatedAt, now));
        target.setSelected(1);
        target.setUpdatedBy(current.userId());
        target.setUpdatedAt(now);
        mapper.updateById(target);
        return toVo(target);
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Transactional
    public AiModelRuntimeConfigVo switchThinking(boolean enabled) {
        CurrentPrincipal current = CurrentUser.require();
        AiModelRuntimeConfigEntity selected =
                mapper.selectOne(
                        new LambdaQueryWrapper<AiModelRuntimeConfigEntity>()
                                .eq(AiModelRuntimeConfigEntity::getStatus, "ACTIVE")
                                .eq(AiModelRuntimeConfigEntity::getSelected, 1)
                                .last("LIMIT 1"));
        if (selected == null) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        if (enabled && (selected.getThinkingSupported() == null || selected.getThinkingSupported() != 1)) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID_STATUS);
        }
        LocalDateTime now = LocalDateTime.now();
        mapper.update(
                null,
                new LambdaUpdateWrapper<AiModelRuntimeConfigEntity>()
                        .eq(AiModelRuntimeConfigEntity::getStatus, "ACTIVE")
                        .set(AiModelRuntimeConfigEntity::getThinkingEnabled, enabled ? 1 : 0)
                        .set(AiModelRuntimeConfigEntity::getUpdatedBy, current.userId())
                        .set(AiModelRuntimeConfigEntity::getUpdatedAt, now));
        selected.setThinkingEnabled(enabled ? 1 : 0);
        selected.setUpdatedBy(current.userId());
        selected.setUpdatedAt(now);
        return toVo(selected);
    }

    private AiModelRuntimeConfigVo toVo(AiModelRuntimeConfigEntity entity) {
        return new AiModelRuntimeConfigVo(
                String.valueOf(entity.getId()),
                entity.getModelCode(),
                entity.getModelName(),
                entity.getProvider(),
                entity.getModelVersion(),
                entity.getBaseUrl(),
                entity.getContextLengthTokens(),
                entity.getMaxOutputTokens(),
                entity.getThinkingSupported() != null && entity.getThinkingSupported() == 1,
                entity.getThinkingEnabled() != null && entity.getThinkingEnabled() == 1,
                entity.getSelected() != null && entity.getSelected() == 1,
                entity.getStatus(),
                entity.getUpdatedAt());
    }
}
