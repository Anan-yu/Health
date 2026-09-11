package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.vo.GoldBeanReferralBranchVo;
import com.rayk.health.goldbean.vo.GoldBeanReferralMemberVo;
import com.rayk.health.goldbean.vo.GoldBeanReferralNetworkVo;
import com.rayk.health.goldbean.vo.GoldBeanReferralTrendPointVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysUserMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoldBeanReferralNetworkService {
    private static final int TREND_DAYS = 7;
    private static final int MAX_VISIBLE_DIRECT = 12;
    private static final int MAX_VISIBLE_CHILDREN = 6;

    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanReferralMapper referralMapper;
    private final SysUserMapper userMapper;

    public GoldBeanReferralNetworkService(
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralMapper referralMapper,
            SysUserMapper userMapper) {
        this.accountMapper = accountMapper;
        this.referralMapper = referralMapper;
        this.userMapper = userMapper;
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional(readOnly = true)
    public GoldBeanReferralNetworkVo network() {
        CurrentPrincipal current = CurrentUser.require();
        GoldBeanAccountEntity selfAccount = accountMapper.selectOne(
                new LambdaQueryWrapper<GoldBeanAccountEntity>()
                        .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                        .eq(GoldBeanAccountEntity::getUserId, current.userId())
                        .eq(GoldBeanAccountEntity::getDeleted, 0)
                        .last("LIMIT 1"));

        List<GoldBeanReferralEntity> directRelations = referralMapper.selectList(
                activeRelations(current.tenantId())
                        .eq(GoldBeanReferralEntity::getReferrerId, current.userId())
                        .orderByDesc(GoldBeanReferralEntity::getRegisteredAt)
                        .orderByDesc(GoldBeanReferralEntity::getId));
        List<Long> directIds = directRelations.stream()
                .map(GoldBeanReferralEntity::getReferredId)
                .distinct()
                .toList();
        List<GoldBeanReferralEntity> secondLevelRelations = directIds.isEmpty()
                ? List.of()
                : referralMapper.selectList(
                        activeRelations(current.tenantId())
                                .in(GoldBeanReferralEntity::getReferrerId, directIds)
                                .orderByDesc(GoldBeanReferralEntity::getRegisteredAt)
                                .orderByDesc(GoldBeanReferralEntity::getId));

        List<GoldBeanReferralEntity> visibleDirect = directRelations.stream()
                .limit(MAX_VISIBLE_DIRECT)
                .toList();
        Set<Long> visibleUserIds = new HashSet<>();
        visibleUserIds.add(current.userId());
        visibleDirect.forEach(relation -> visibleUserIds.add(relation.getReferredId()));

        Map<Long, List<GoldBeanReferralEntity>> secondLevelByReferrer = secondLevelRelations.stream()
                .collect(Collectors.groupingBy(GoldBeanReferralEntity::getReferrerId));
        visibleDirect.forEach(relation -> secondLevelByReferrer
                .getOrDefault(relation.getReferredId(), List.of())
                .stream()
                .limit(MAX_VISIBLE_CHILDREN)
                .forEach(child -> visibleUserIds.add(child.getReferredId())));

        Map<Long, SysUserEntity> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUserEntity>()
                                .eq(SysUserEntity::getTenantId, current.tenantId())
                                .in(SysUserEntity::getId, visibleUserIds)
                                .eq(SysUserEntity::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));
        Map<Long, GoldBeanAccountEntity> accounts = accountMapper.selectList(
                        new LambdaQueryWrapper<GoldBeanAccountEntity>()
                                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                                .in(GoldBeanAccountEntity::getUserId, visibleUserIds)
                                .eq(GoldBeanAccountEntity::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(GoldBeanAccountEntity::getUserId, Function.identity()));

        List<GoldBeanReferralBranchVo> branches = new ArrayList<>();
        for (GoldBeanReferralEntity direct : visibleDirect) {
            long directUserId = direct.getReferredId();
            List<GoldBeanReferralEntity> allChildren = secondLevelByReferrer
                    .getOrDefault(directUserId, List.of());
            List<GoldBeanReferralMemberVo> children = allChildren.stream()
                    .limit(MAX_VISIBLE_CHILDREN)
                    .map(child -> new GoldBeanReferralMemberVo(
                            displayName(users.get(child.getReferredId())),
                            levelName(accounts.get(child.getReferredId()))))
                    .toList();
            GoldBeanAccountEntity directAccount = accounts.get(directUserId);
            branches.add(new GoldBeanReferralBranchVo(
                    displayName(users.get(directUserId)),
                    levelName(directAccount),
                    Math.max(accountReferralCount(directAccount), allChildren.size()),
                    Math.max(0, allChildren.size() - children.size()),
                    children));
        }

        int directReferralCount = Math.max(accountReferralCount(selfAccount), directRelations.size());
        int teamMemberCount = directReferralCount + secondLevelRelations.size();
        return new GoldBeanReferralNetworkVo(
                displayName(users.get(current.userId())),
                levelName(selfAccount),
                directReferralCount,
                teamMemberCount,
                Math.max(0, directReferralCount - branches.size()),
                branches,
                trend(directRelations, directReferralCount));
    }

    private LambdaQueryWrapper<GoldBeanReferralEntity> activeRelations(long tenantId) {
        return new LambdaQueryWrapper<GoldBeanReferralEntity>()
                .eq(GoldBeanReferralEntity::getTenantId, tenantId)
                .eq(GoldBeanReferralEntity::getStatus, "ACTIVE")
                .eq(GoldBeanReferralEntity::getDeleted, 0);
    }

    private List<GoldBeanReferralTrendPointVo> trend(
            List<GoldBeanReferralEntity> directRelations,
            int directReferralCount) {
        LocalDate today = LocalDate.now();
        long datedRelationCount = directRelations.stream()
                .filter(relation -> relation.getRegisteredAt() != null
                        && !relation.getRegisteredAt().toLocalDate().isAfter(today))
                .count();
        int historicalBaseline = Math.max(0, directReferralCount - (int) datedRelationCount);
        List<GoldBeanReferralTrendPointVo> result = new ArrayList<>(TREND_DAYS);
        for (int offset = TREND_DAYS - 1; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            int count = historicalBaseline + (int) directRelations.stream()
                    .filter(relation -> relation.getRegisteredAt() != null)
                    .filter(relation -> !relation.getRegisteredAt().toLocalDate().isAfter(date))
                    .count();
            result.add(new GoldBeanReferralTrendPointVo(
                    date,
                    count,
                    GoldBeanLevel.forDirectReferralCount(count).displayName()));
        }
        return result;
    }

    private int accountReferralCount(GoldBeanAccountEntity account) {
        return account == null || account.getDirectReferralCount() == null
                ? 0
                : Math.max(0, account.getDirectReferralCount());
    }

    private String levelName(GoldBeanAccountEntity account) {
        return account == null
                ? GoldBeanLevel.ORDINARY.displayName()
                : GoldBeanLevel.fromCode(account.getMemberLevel()).displayName();
    }

    private String displayName(SysUserEntity user) {
        return user != null && StringUtils.hasText(user.getDisplayName())
                ? user.getDisplayName().trim()
                : "会员";
    }
}
