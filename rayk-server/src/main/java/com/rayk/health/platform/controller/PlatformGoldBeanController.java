package com.rayk.health.platform.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.goldbean.application.GoldBeanPlatformInviteService;
import com.rayk.health.goldbean.dto.CreateGoldBeanPlatformInviteRequest;
import com.rayk.health.goldbean.application.GoldBeanLegendaryService;
import com.rayk.health.goldbean.dto.CreateGoldBeanLegendaryRequest;
import com.rayk.health.goldbean.application.GoldRegionProfitService;
import com.rayk.health.platform.application.PlatformGoldBeanService;
import com.rayk.health.platform.vo.PlatformGoldBeanAccountVo;
import com.rayk.health.platform.vo.PlatformGoldBeanInviteCreatedVo;
import com.rayk.health.platform.vo.PlatformGoldBeanInviteVo;
import com.rayk.health.platform.vo.PlatformGoldBeanLedgerVo;
import com.rayk.health.platform.vo.PlatformGoldBeanOverviewVo;
import com.rayk.health.platform.vo.PlatformGoldBeanOrderVo;
import com.rayk.health.platform.vo.PlatformGoldBeanReferralVo;
import com.rayk.health.platform.vo.PlatformGoldBeanLegendaryVo;
import com.rayk.health.platform.vo.PlatformGoldRegionProfitVo;
import com.rayk.health.platform.vo.PlatformGoldRegionVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Development-only operations APIs for the platform gold-bean operations console. */
@RestController
@RequestMapping("/api/v1/platform/gold-bean")
public class PlatformGoldBeanController {
    private final PlatformGoldBeanService service;
    private final GoldBeanPlatformInviteService inviteService;
    private final GoldBeanLegendaryService legendaryService;
    private final GoldRegionProfitService regionProfitService;

    public PlatformGoldBeanController(
            PlatformGoldBeanService service,
            GoldBeanPlatformInviteService inviteService,
            GoldBeanLegendaryService legendaryService,
            GoldRegionProfitService regionProfitService) {
        this.service = service;
        this.inviteService = inviteService;
        this.legendaryService = legendaryService;
        this.regionProfitService = regionProfitService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PlatformGoldBeanOverviewVo> overview() {
        return ApiResponse.success(service.overview());
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldBeanAccountVo>> accounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String registrationStatus) {
        return ApiResponse.success(service.accounts(keyword, registrationStatus));
    }

    @GetMapping("/referrals")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldBeanReferralVo>> referrals(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(service.referrals(keyword));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldBeanLedgerVo>> ledger(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String eventType) {
        return ApiResponse.success(service.ledger(keyword, eventType));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldBeanOrderVo>> orders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType) {
        return ApiResponse.success(service.orders(keyword, status, orderType));
    }

    @GetMapping("/platform-invites")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldBeanInviteVo>> platformInvites(
            @RequestParam(required = false) String status) {
        return ApiResponse.success(inviteService.list(status));
    }

    @PostMapping("/platform-invites")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PlatformGoldBeanInviteCreatedVo> createPlatformInvite(
            @Valid @RequestBody(required = false) CreateGoldBeanPlatformInviteRequest request) {
        return ApiResponse.success(inviteService.issue(request));
    }

    @PostMapping("/platform-invites/{inviteId}/revoke")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PlatformGoldBeanInviteVo> revokePlatformInvite(@PathVariable String inviteId) {
        return ApiResponse.success(inviteService.revoke(inviteId));
    }

    @GetMapping("/legendary")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldBeanLegendaryVo>> legendary() {
        return ApiResponse.success(legendaryService.list());
    }

    @PostMapping("/legendary")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PlatformGoldBeanLegendaryVo> createLegendary(
            @Valid @RequestBody CreateGoldBeanLegendaryRequest request) {
        return ApiResponse.success(legendaryService.create(request));
    }

    @PostMapping("/legendary/{id}/revoke")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PlatformGoldBeanLegendaryVo> revokeLegendary(@PathVariable String id) {
        return ApiResponse.success(legendaryService.revoke(id));
    }

    @GetMapping("/regions")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldRegionVo>> regions() {
        return ApiResponse.success(regionProfitService.regions());
    }

    @GetMapping("/region-profits")
    @PreAuthorize("hasAuthority('platform:tenant:list')")
    public ApiResponse<PageResponse<PlatformGoldRegionProfitVo>> regionProfits() {
        return ApiResponse.success(regionProfitService.profits());
    }

}
