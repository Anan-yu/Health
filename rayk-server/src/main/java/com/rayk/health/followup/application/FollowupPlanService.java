package com.rayk.health.followup.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.followup.dto.CreateFollowupPlanRequest;
import com.rayk.health.followup.entity.FollowupPlanEntity;
import com.rayk.health.followup.entity.FollowupTaskEntity;
import com.rayk.health.followup.mapper.FollowupPlanMapper;
import com.rayk.health.followup.mapper.FollowupTaskMapper;
import com.rayk.health.followup.vo.FollowupPlanVo;
import com.rayk.health.followup.vo.FollowupTaskVo;
import com.rayk.health.patient.application.DataScopeService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowupPlanService {
    private final FollowupPlanMapper planMapper;
    private final FollowupTaskMapper taskMapper;
    private final DataScopeService dataScopeService;

    public FollowupPlanService(
            FollowupPlanMapper planMapper,
            FollowupTaskMapper taskMapper,
            DataScopeService dataScopeService) {
        this.planMapper = planMapper;
        this.taskMapper = taskMapper;
        this.dataScopeService = dataScopeService;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('followup:manage', 'followup:create')")
    public FollowupPlanVo createPlan(CreateFollowupPlanRequest request) {
        dataScopeService.requirePatient(request.patientId());
        CurrentPrincipal current = CurrentUser.require();
        LocalDateTime now = LocalDateTime.now();

        LocalDate endDate = request.startDate().plusDays((long) request.intervalDays() * (request.totalOccurrences() - 1));

        FollowupPlanEntity plan = new FollowupPlanEntity();
        plan.setTenantId(current.tenantId());
        plan.setPatientId(request.patientId());
        plan.setPlanName(request.planName());
        plan.setStartDate(request.startDate());
        plan.setEndDate(endDate);
        plan.setStatus("DRAFT");
        plan.setCreatedBy(current.userId());
        plan.setCreatedAt(now);
        plan.setUpdatedBy(current.userId());
        plan.setUpdatedAt(now);
        plan.setDeleted(0);
        plan.setVersion(0);
        planMapper.insert(plan);

        // Auto-generate followup tasks at each interval
        for (int i = 0; i < request.totalOccurrences(); i++) {
            LocalDate dueDate = request.startDate().plusDays((long) request.intervalDays() * i);
            FollowupTaskEntity task = new FollowupTaskEntity();
            task.setTenantId(current.tenantId());
            task.setPatientId(request.patientId());
            task.setPlanId(plan.getId());
            task.setAssigneeId(request.assigneeId());
            task.setTitle(request.title() + " (" + (i + 1) + "/" + request.totalOccurrences() + ")");
            task.setContent(request.content());
            task.setDueDate(dueDate);
            task.setStatus("DRAFT");
            task.setCreatedBy(current.userId());
            task.setCreatedAt(now);
            task.setUpdatedBy(current.userId());
            task.setUpdatedAt(now);
            task.setDeleted(0);
            task.setVersion(0);
            taskMapper.insert(task);
        }

        return toPlanVo(plan);
    }

    @PreAuthorize("hasAnyAuthority('followup:manage', 'followup:create')")
    public List<FollowupPlanVo> listPlans(long patientId) {
        dataScopeService.requirePatient(patientId);
        return planMapper.selectList(
                new LambdaQueryWrapper<FollowupPlanEntity>()
                        .eq(FollowupPlanEntity::getPatientId, patientId)
                        .eq(FollowupPlanEntity::getDeleted, 0)
                        .orderByDesc(FollowupPlanEntity::getCreatedAt))
                .stream()
                .map(this::toPlanVo)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('followup:manage', 'followup:create')")
    @Transactional
    public FollowupPlanVo activatePlan(long planId) {
        FollowupPlanEntity plan = requirePlan(planId);
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.FOLLOWUP_INVALID_STATUS);
        }
        CurrentPrincipal current = CurrentUser.require();
        plan.setStatus("ACTIVE");
        plan.setUpdatedBy(current.userId());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
        taskMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<
                                FollowupTaskEntity>()
                        .eq(FollowupTaskEntity::getPlanId, planId)
                        .eq(FollowupTaskEntity::getStatus, "DRAFT")
                        .set(FollowupTaskEntity::getStatus, "PENDING")
                        .set(FollowupTaskEntity::getUpdatedBy, current.userId())
                        .set(FollowupTaskEntity::getUpdatedAt, LocalDateTime.now()));
        return toPlanVo(plan);
    }

    @PreAuthorize("hasAnyAuthority('followup:manage', 'followup:create')")
    public FollowupPlanVo completePlan(long planId) {
        FollowupPlanEntity plan = requirePlan(planId);
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.FOLLOWUP_INVALID_STATUS);
        }
        CurrentPrincipal current = CurrentUser.require();
        plan.setStatus("COMPLETED");
        plan.setUpdatedBy(current.userId());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
        return toPlanVo(plan);
    }

    private FollowupPlanEntity requirePlan(long planId) {
        FollowupPlanEntity plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.FOLLOWUP_NOT_FOUND);
        }
        dataScopeService.requirePatient(plan.getPatientId());
        return plan;
    }

    private FollowupPlanVo toPlanVo(FollowupPlanEntity plan) {
        List<FollowupTaskVo> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<FollowupTaskEntity>()
                        .eq(FollowupTaskEntity::getPlanId, plan.getId())
                        .eq(FollowupTaskEntity::getDeleted, 0)
                        .orderByAsc(FollowupTaskEntity::getDueDate))
                .stream()
                .map(this::toTaskVo)
                .toList();
        return new FollowupPlanVo(
                String.valueOf(plan.getId()),
                String.valueOf(plan.getPatientId()),
                plan.getPlanName(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus(),
                tasks,
                plan.getCreatedAt());
    }

    private FollowupTaskVo toTaskVo(FollowupTaskEntity task) {
        return new FollowupTaskVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getPatientId()),
                task.getTitle(),
                task.getContent(),
                task.getDueDate(),
                task.getStatus(),
                task.getFeedback(),
                task.getCompletedAt());
    }
}
