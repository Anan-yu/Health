package com.rayk.health.goldbean.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.goldbean.application.GoldBeanApplicationService;
import com.rayk.health.goldbean.application.GoldBeanLegendaryService;
import com.rayk.health.goldbean.application.GoldBeanPaymentService;
import com.rayk.health.goldbean.application.GoldBeanReferralAuthorizationService;
import com.rayk.health.goldbean.application.GoldBeanReferralNetworkService;
import com.rayk.health.goldbean.application.GoldBeanRobotService;
import com.rayk.health.goldbean.application.GoldBeanTradeService;
import com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest;
import com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest;
import com.rayk.health.goldbean.dto.CreateGoldBeanPurchaseOrderRequest;
import com.rayk.health.goldbean.dto.CreateGoldBeanRegistrationOrderRequest;
import com.rayk.health.goldbean.dto.OpenGoldRegionRequest;
import com.rayk.health.goldbean.dto.RedeemGoldRobotRequest;
import com.rayk.health.goldbean.dto.RegisterGoldBeanRequest;
import com.rayk.health.goldbean.vo.GoldBeanLedgerVo;
import com.rayk.health.goldbean.vo.GoldBeanLegendarySummaryVo;
import com.rayk.health.goldbean.vo.GoldBeanOrderVo;
import com.rayk.health.goldbean.vo.GoldBeanReferralSettlementVo;
import com.rayk.health.goldbean.vo.GoldBeanReferralAuthorizationVo;
import com.rayk.health.goldbean.vo.GoldBeanReferralNetworkVo;
import com.rayk.health.goldbean.vo.GoldBeanSummaryVo;
import com.rayk.health.goldbean.vo.GoldBeanTradeListingVo;
import com.rayk.health.goldbean.vo.GoldBeanTradePayoutVo;
import com.rayk.health.goldbean.vo.GoldBeanTradeVo;
import com.rayk.health.goldbean.vo.GoldRegionVo;
import com.rayk.health.goldbean.vo.GoldRobotRedemptionVo;
import com.rayk.health.goldbean.vo.GoldRobotStatusVo;
import com.rayk.health.membership.vo.MembershipPaymentVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/gold-bean")
@PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
public class GoldBeanController {
    private final GoldBeanApplicationService service;
    private final GoldBeanPaymentService paymentService;
    private final GoldBeanReferralAuthorizationService referralAuthorizationService;
    private final GoldBeanReferralNetworkService referralNetworkService;
    private final GoldBeanTradeService tradeService;
    private final GoldBeanLegendaryService legendaryService;
    private final GoldBeanRobotService robotService;

    public GoldBeanController(
            GoldBeanApplicationService service,
            GoldBeanPaymentService paymentService,
            GoldBeanReferralAuthorizationService referralAuthorizationService,
            GoldBeanReferralNetworkService referralNetworkService,
            GoldBeanTradeService tradeService,
            GoldBeanLegendaryService legendaryService,
            GoldBeanRobotService robotService) {
        this.service = service;
        this.paymentService = paymentService;
        this.referralAuthorizationService = referralAuthorizationService;
        this.referralNetworkService = referralNetworkService;
        this.tradeService = tradeService;
        this.legendaryService = legendaryService;
        this.robotService = robotService;
    }

    @GetMapping("/summary")
    public ApiResponse<GoldBeanSummaryVo> summary() {
        return ApiResponse.success(service.summary());
    }

    @GetMapping("/referral-network")
    public ApiResponse<GoldBeanReferralNetworkVo> referralNetwork() {
        return ApiResponse.success(referralNetworkService.network());
    }

    @PostMapping("/register")
    public ApiResponse<GoldBeanSummaryVo> register(
            @Valid @RequestBody(required = false) RegisterGoldBeanRequest request) {
        return ApiResponse.success(service.register(request));
    }

    @PostMapping("/registration-orders")
    public ApiResponse<GoldBeanOrderVo> createRegistrationOrder(
            @Valid @RequestBody(required = false) CreateGoldBeanRegistrationOrderRequest request) {
        return ApiResponse.success(paymentService.createRegistrationOrder(request));
    }

    @PostMapping("/purchase-orders")
    public ApiResponse<GoldBeanOrderVo> createPurchaseOrder(
            @Valid @RequestBody CreateGoldBeanPurchaseOrderRequest request) {
        return ApiResponse.success(paymentService.createPurchaseOrder(request));
    }

    @GetMapping("/legendary/summary")
    public ApiResponse<GoldBeanLegendarySummaryVo> legendarySummary() {
        return ApiResponse.success(legendaryService.summary());
    }

    @GetMapping("/robot/status")
    public ApiResponse<GoldRobotStatusVo> robotStatus() {
        return ApiResponse.success(robotService.status());
    }

    @PostMapping("/robot/redeem")
    public ApiResponse<GoldRobotRedemptionVo> redeemRobot(
            @Valid @RequestBody RedeemGoldRobotRequest request) {
        return ApiResponse.success(robotService.redeem(request));
    }

    @PostMapping("/legendary/purchase-orders")
    public ApiResponse<GoldBeanOrderVo> createLegendaryBankPurchaseOrder(
            @Valid @RequestBody CreateGoldBeanPurchaseOrderRequest request) {
        return ApiResponse.success(paymentService.createLegendaryBankPurchaseOrder(request));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<GoldBeanOrderVo> order(@PathVariable String orderNo) {
        return ApiResponse.success(paymentService.order(orderNo));
    }

    @PostMapping("/orders/{orderNo}/wechat-pay")
    public ApiResponse<MembershipPaymentVo> wechatPay(@PathVariable String orderNo) {
        return ApiResponse.success(paymentService.createWechatPayment(orderNo));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public ApiResponse<GoldBeanOrderVo> cancelOrder(@PathVariable String orderNo) {
        return ApiResponse.success(paymentService.cancelOrder(orderNo));
    }

    @GetMapping("/referral-settlements")
    public ApiResponse<List<GoldBeanReferralSettlementVo>> referralSettlements() {
        return ApiResponse.success(paymentService.referralSettlements());
    }

    @PostMapping("/referral-settlements/{orderNo}/sync")
    public ApiResponse<GoldBeanReferralSettlementVo> syncReferralSettlement(@PathVariable String orderNo) {
        return ApiResponse.success(paymentService.syncReferralSettlementForReferrer(orderNo));
    }

    @GetMapping("/referral-receive-authorization")
    public ApiResponse<GoldBeanReferralAuthorizationVo> referralReceiveAuthorization() {
        return ApiResponse.success(referralAuthorizationService.status());
    }

    @PostMapping("/referral-receive-authorization")
    public ApiResponse<GoldBeanReferralAuthorizationVo> beginReferralReceiveAuthorization() {
        return ApiResponse.success(referralAuthorizationService.beginAuthorization());
    }

    @PostMapping("/referral-receive-authorization/sync")
    public ApiResponse<GoldBeanReferralAuthorizationVo> syncReferralReceiveAuthorization() {
        return ApiResponse.success(referralAuthorizationService.syncAuthorization());
    }

    @GetMapping("/trade/market")
    public ApiResponse<PageResponse<GoldBeanTradeListingVo>> tradeMarket(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return ApiResponse.success(tradeService.market(page, size));
    }

    @GetMapping("/trade/listings/mine")
    public ApiResponse<PageResponse<GoldBeanTradeListingVo>> myTradeListings(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return ApiResponse.success(tradeService.mine(page, size));
    }

    @PostMapping("/trade/listings")
    public ApiResponse<GoldBeanTradeListingVo> createTradeListing(
            @Valid @RequestBody CreateGoldBeanTradeListingRequest request) {
        return ApiResponse.success(tradeService.create(request));
    }

    @PostMapping("/trade/listings/{listingId}/buy")
    public ApiResponse<GoldBeanTradeVo> buyTradeListing(
            @PathVariable String listingId,
            @Valid @RequestBody BuyGoldBeanTradeRequest request) {
        return ApiResponse.success(tradeService.buy(listingId, request));
    }

    @GetMapping("/trade/orders/{tradeNo}")
    public ApiResponse<GoldBeanTradeVo> trade(@PathVariable String tradeNo) {
        return ApiResponse.success(tradeService.trade(tradeNo));
    }

    @PostMapping("/trade/orders/{tradeNo}/wechat-pay")
    public ApiResponse<MembershipPaymentVo> tradeWechatPay(@PathVariable String tradeNo) {
        return ApiResponse.success(tradeService.createWechatPayment(tradeNo));
    }

    @PostMapping("/trade/orders/{tradeNo}/cancel")
    public ApiResponse<GoldBeanTradeVo> cancelTrade(@PathVariable String tradeNo) {
        return ApiResponse.success(tradeService.cancelTrade(tradeNo));
    }

    @GetMapping("/trade/payouts")
    public ApiResponse<List<GoldBeanTradePayoutVo>> tradePayouts() {
        return ApiResponse.success(tradeService.sellerPayouts());
    }

    @PostMapping("/trade/payouts/{tradeNo}/sync")
    public ApiResponse<GoldBeanTradePayoutVo> syncTradePayout(@PathVariable String tradeNo) {
        return ApiResponse.success(tradeService.syncPayoutForSeller(tradeNo));
    }

    @PostMapping("/trade/listings/{listingId}/cancel")
    public ApiResponse<GoldBeanTradeListingVo> cancelTradeListing(@PathVariable String listingId) {
        return ApiResponse.success(tradeService.cancel(listingId));
    }

    @GetMapping("/ledger")
    public ApiResponse<List<GoldBeanLedgerVo>> ledger() {
        return ApiResponse.success(service.ledger());
    }

    @PostMapping("/regions")
    public ApiResponse<GoldRegionVo> openRegion(@Valid @RequestBody OpenGoldRegionRequest request) {
        return ApiResponse.success(service.openRegion(request));
    }

    @GetMapping("/regions/mine")
    public ApiResponse<GoldRegionVo> myRegion() {
        return ApiResponse.success(service.myRegion());
    }
}
