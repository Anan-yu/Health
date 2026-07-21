package com.rayk.health.indicator.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.indicator.dto.AddAliasRequest;
import com.rayk.health.indicator.dto.CreateIndicatorRequest;
import com.rayk.health.indicator.dto.UpdateIndicatorRequest;
import com.rayk.health.indicator.entity.IndicatorAliasEntity;
import com.rayk.health.indicator.entity.IndicatorDictEntity;
import com.rayk.health.indicator.mapper.IndicatorAliasMapper;
import com.rayk.health.indicator.mapper.IndicatorDictMapper;
import com.rayk.health.indicator.vo.IndicatorAliasVo;
import com.rayk.health.indicator.vo.IndicatorDictVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class IndicatorDictService {
    private final IndicatorDictMapper dictMapper;
    private final IndicatorAliasMapper aliasMapper;

    public IndicatorDictService(IndicatorDictMapper dictMapper, IndicatorAliasMapper aliasMapper) {
        this.dictMapper = dictMapper;
        this.aliasMapper = aliasMapper;
    }

    public List<IndicatorDictVo> listIndicators(String categoryCode) {
        LambdaQueryWrapper<IndicatorDictEntity> query =
                new LambdaQueryWrapper<IndicatorDictEntity>()
                        .eq(IndicatorDictEntity::getEnabled, 1)
                        .eq(categoryCode != null && !categoryCode.isBlank(),
                                IndicatorDictEntity::getCategoryCode, categoryCode)
                        .orderByAsc(IndicatorDictEntity::getCategoryCode)
                        .orderByAsc(IndicatorDictEntity::getIndicatorCode);
        return dictMapper.selectList(query).stream().map(this::toVo).toList();
    }

    public IndicatorDictVo getIndicator(String code) {
        return toVo(requireByCode(code));
    }

    @PreAuthorize("hasAuthority('indicator:manage')")
    public IndicatorDictVo createIndicator(CreateIndicatorRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        Long existing =
                dictMapper.selectCount(
                        new LambdaQueryWrapper<IndicatorDictEntity>()
                                .eq(IndicatorDictEntity::getIndicatorCode, request.indicatorCode()));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.INDICATOR_CODE_DUPLICATE);
        }
        IndicatorDictEntity entity = new IndicatorDictEntity();
        entity.setTenantId(current.tenantId());
        entity.setIndicatorCode(request.indicatorCode());
        entity.setIndicatorName(request.indicatorName());
        entity.setStandardUnit(request.standardUnit());
        entity.setCategoryCode(request.categoryCode());
        entity.setEnabled(1);
        entity.setCreatedBy(current.userId());
        entity.setUpdatedBy(current.userId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setVersion(0);
        dictMapper.insert(entity);
        return toVo(entity);
    }

    @PreAuthorize("hasAuthority('indicator:manage')")
    public IndicatorDictVo updateIndicator(String code, UpdateIndicatorRequest request) {
        IndicatorDictEntity entity = requireByCode(code);
        CurrentPrincipal current = CurrentUser.require();
        if (request.indicatorName() != null && !request.indicatorName().isBlank()) {
            entity.setIndicatorName(request.indicatorName());
        }
        if (request.standardUnit() != null && !request.standardUnit().isBlank()) {
            entity.setStandardUnit(request.standardUnit());
        }
        if (request.categoryCode() != null && !request.categoryCode().isBlank()) {
            entity.setCategoryCode(request.categoryCode());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        entity.setUpdatedBy(current.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        dictMapper.updateById(entity);
        return toVo(entity);
    }

    @PreAuthorize("hasAuthority('indicator:manage')")
    public IndicatorAliasVo addAlias(String indicatorCode, AddAliasRequest request) {
        IndicatorDictEntity dict = requireByCode(indicatorCode);
        CurrentPrincipal current = CurrentUser.require();
        Long existing =
                aliasMapper.selectCount(
                        new LambdaQueryWrapper<IndicatorAliasEntity>()
                                .eq(IndicatorAliasEntity::getAliasName, request.aliasName()));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.INDICATOR_ALIAS_DUPLICATE);
        }
        IndicatorAliasEntity alias = new IndicatorAliasEntity();
        alias.setTenantId(current.tenantId());
        alias.setIndicatorId(dict.getId());
        alias.setAliasName(request.aliasName());
        alias.setSource(request.source());
        alias.setCreatedBy(current.userId());
        alias.setUpdatedBy(current.userId());
        alias.setCreatedAt(LocalDateTime.now());
        alias.setUpdatedAt(LocalDateTime.now());
        alias.setDeleted(0);
        alias.setVersion(0);
        aliasMapper.insert(alias);
        return toAliasVo(alias, indicatorCode);
    }

    public List<IndicatorAliasVo> listAliases(String indicatorCode) {
        IndicatorDictEntity dict = requireByCode(indicatorCode);
        return aliasMapper
                .selectList(
                        new LambdaQueryWrapper<IndicatorAliasEntity>()
                                .eq(IndicatorAliasEntity::getIndicatorId, dict.getId())
                                .orderByAsc(IndicatorAliasEntity::getCreatedAt))
                .stream()
                .map(alias -> toAliasVo(alias, indicatorCode))
                .toList();
    }

    public IndicatorDictVo resolveByAlias(String aliasName) {
        IndicatorAliasEntity alias =
                aliasMapper.selectOne(
                        new LambdaQueryWrapper<IndicatorAliasEntity>()
                                .eq(IndicatorAliasEntity::getAliasName, aliasName)
                                .last("LIMIT 1"));
        if (alias == null) {
            throw new BusinessException(ErrorCode.INDICATOR_NOT_FOUND);
        }
        IndicatorDictEntity dict = dictMapper.selectById(alias.getIndicatorId());
        if (dict == null) {
            throw new BusinessException(ErrorCode.INDICATOR_NOT_FOUND);
        }
        return toVo(dict);
    }

    private IndicatorDictEntity requireByCode(String code) {
        IndicatorDictEntity entity =
                dictMapper.selectOne(
                        new LambdaQueryWrapper<IndicatorDictEntity>()
                                .eq(IndicatorDictEntity::getIndicatorCode, code)
                                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.INDICATOR_NOT_FOUND);
        }
        return entity;
    }

    private IndicatorDictVo toVo(IndicatorDictEntity entity) {
        return new IndicatorDictVo(
                String.valueOf(entity.getId()),
                entity.getIndicatorCode(),
                entity.getIndicatorName(),
                entity.getStandardUnit(),
                entity.getCategoryCode(),
                entity.getEnabled() != null && entity.getEnabled() == 1,
                entity.getCreatedAt());
    }

    private IndicatorAliasVo toAliasVo(IndicatorAliasEntity alias, String indicatorCode) {
        return new IndicatorAliasVo(
                String.valueOf(alias.getId()),
                indicatorCode,
                alias.getAliasName(),
                alias.getSource(),
                alias.getCreatedAt());
    }
}
