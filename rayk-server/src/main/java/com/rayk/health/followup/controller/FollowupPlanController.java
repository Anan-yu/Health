package com.rayk.health.followup.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.followup.application.FollowupPlanService;
import com.rayk.health.followup.dto.CreateFollowupPlanRequest;
import com.rayk.health.followup.vo.FollowupPlanVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/followup-plans")
public class FollowupPlanController {
    private final FollowupPlanService planService;

    public FollowupPlanController(FollowupPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ApiResponse<FollowupPlanVo> create(@Valid @RequestBody CreateFollowupPlanRequest request) {
        return ApiResponse.success(planService.createPlan(request));
    }

    @GetMapping
    public ApiResponse<List<FollowupPlanVo>> list(@RequestParam long patientId) {
        return ApiResponse.success(planService.listPlans(patientId));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<FollowupPlanVo> activate(@PathVariable long id) {
        return ApiResponse.success(planService.activatePlan(id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<FollowupPlanVo> complete(@PathVariable long id) {
        return ApiResponse.success(planService.completePlan(id));
    }
}
