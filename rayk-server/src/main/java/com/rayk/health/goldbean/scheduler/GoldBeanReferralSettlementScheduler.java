package com.rayk.health.goldbean.scheduler;

import com.rayk.health.goldbean.application.GoldBeanPaymentService;
import com.rayk.health.goldbean.application.GoldBeanReferralAuthorizationService;
import com.rayk.health.goldbean.application.GoldBeanTradeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically reconciles accepted referral payouts without creating new bill numbers. */
@Component
public class GoldBeanReferralSettlementScheduler {
    private final GoldBeanPaymentService paymentService;
    private final GoldBeanReferralAuthorizationService authorizationService;
    private final GoldBeanTradeService tradeService;

    public GoldBeanReferralSettlementScheduler(
            GoldBeanPaymentService paymentService,
            GoldBeanReferralAuthorizationService authorizationService,
            GoldBeanTradeService tradeService) {
        this.paymentService = paymentService;
        this.authorizationService = authorizationService;
        this.tradeService = tradeService;
    }

    @Scheduled(fixedDelayString = "${rayk.gold-bean.registration-referral-settlement-recovery-interval-ms:10000}")
    public void recoverPendingSettlements() {
        authorizationService.recoverPendingAuthorizations();
        paymentService.recoverPendingReferralSettlements();
        tradeService.recoverPendingTrades();
    }
}
