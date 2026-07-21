package com.rayk.health.indicator.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.indicator.dto.CreateModelRequest;
import com.rayk.health.indicator.entity.HealthModelConfigEntity;
import com.rayk.health.indicator.mapper.HealthModelConfigMapper;
import com.rayk.health.indicator.vo.HealthModelConfigVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentModelService {
    private final HealthModelConfigMapper modelConfigMapper;

    public AssessmentModelService(HealthModelConfigMapper modelConfigMapper) {
        this.modelConfigMapper = modelConfigMapper;
    }

    public List<HealthModelConfigVo> listActiveModels() {
        LambdaQueryWrapper<HealthModelConfigEntity> query =
                new LambdaQueryWrapper<HealthModelConfigEntity>()
                        .eq(HealthModelConfigEntity::getStatus, "ACTIVE")
                        .orderByAsc(HealthModelConfigEntity::getModelCode);
        return modelConfigMapper.selectList(query).stream().map(this::toVo).toList();
    }

    /** Returns the model codes that are active for the current tenant. */
    public List<String> activeModelCodes() {
        return modelConfigMapper
                .selectList(
                        new LambdaQueryWrapper<HealthModelConfigEntity>()
                                .eq(HealthModelConfigEntity::getStatus, "ACTIVE")
                                .orderByAsc(HealthModelConfigEntity::getModelCode))
                .stream()
                .map(HealthModelConfigEntity::getModelCode)
                .toList();
    }

    public HealthModelConfigVo getModel(String modelCode) {
        HealthModelConfigEntity entity =
                modelConfigMapper.selectOne(
                        new LambdaQueryWrapper<HealthModelConfigEntity>()
                                .eq(HealthModelConfigEntity::getModelCode, modelCode)
                                .eq(HealthModelConfigEntity::getStatus, "ACTIVE")
                                .orderByDesc(HealthModelConfigEntity::getCreatedAt)
                                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        return toVo(entity);
    }

    @PreAuthorize("hasAuthority('indicator:manage')")
    public HealthModelConfigVo createModelVersion(CreateModelRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        Long existing =
                modelConfigMapper.selectCount(
                        new LambdaQueryWrapper<HealthModelConfigEntity>()
                                .eq(HealthModelConfigEntity::getModelCode, request.modelCode())
                                .eq(HealthModelConfigEntity::getVersion, request.version()));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID_STATUS);
        }
        HealthModelConfigEntity entity = new HealthModelConfigEntity();
        entity.setTenantId(current.tenantId());
        entity.setModelCode(request.modelCode());
        entity.setModelName(request.modelName());
        entity.setModelCategory(request.modelCategory());
        entity.setVersion(request.version());
        entity.setStatus("DRAFT");
        entity.setConfigSnapshot(
                request.configSnapshot() != null ? request.configSnapshot() : "{}");
        entity.setCreatedBy(current.userId());
        entity.setUpdatedBy(current.userId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setOptimisticVersion(0);
        modelConfigMapper.insert(entity);
        return toVo(entity);
    }

    @PreAuthorize("hasAuthority('indicator:manage')")
    @Transactional
    public HealthModelConfigVo activateModel(String modelCode, String version) {
        CurrentPrincipal current = CurrentUser.require();
        HealthModelConfigEntity target =
                modelConfigMapper.selectOne(
                        new LambdaQueryWrapper<HealthModelConfigEntity>()
                                .eq(HealthModelConfigEntity::getModelCode, modelCode)
                                .eq(HealthModelConfigEntity::getVersion, version)
                                .last("LIMIT 1"));
        if (target == null) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND);
        }
        if (!"DRAFT".equals(target.getStatus())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID_STATUS);
        }
        // Deprecate all currently ACTIVE versions of this model
        modelConfigMapper.update(
                null,
                new LambdaUpdateWrapper<HealthModelConfigEntity>()
                        .eq(HealthModelConfigEntity::getModelCode, modelCode)
                        .eq(HealthModelConfigEntity::getStatus, "ACTIVE")
                        .set(HealthModelConfigEntity::getStatus, "DEPRECATED")
                        .set(HealthModelConfigEntity::getUpdatedBy, current.userId())
                        .set(HealthModelConfigEntity::getUpdatedAt, LocalDateTime.now()));
        // Activate the target version
        target.setStatus("ACTIVE");
        target.setUpdatedBy(current.userId());
        target.setUpdatedAt(LocalDateTime.now());
        modelConfigMapper.updateById(target);
        return toVo(target);
    }

    private HealthModelConfigVo toVo(HealthModelConfigEntity entity) {
        return new HealthModelConfigVo(
                String.valueOf(entity.getId()),
                entity.getModelCode(),
                entity.getModelName(),
                entity.getModelCategory(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getConfigSnapshot(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
