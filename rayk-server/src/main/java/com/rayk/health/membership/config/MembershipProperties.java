package com.rayk.health.membership.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rayk.membership")
public record MembershipProperties(
        boolean enabled,
        boolean paymentEnabled,
        int freeAiAssessmentTrial,
        int freeAiReportTrial,
        int freeAiFollowupTrial,
        int freeHealthShotTrial,
        int freeTtsMealTrial,
        int freeTtsSleepTrial,
        boolean developmentMode,
        WeChatPayProperties wechatPay,
        WeChatVirtualPayProperties wechatVirtualPay) {

    public MembershipProperties {
        wechatPay = wechatPay == null ? WeChatPayProperties.empty() : wechatPay;
        wechatVirtualPay =
                wechatVirtualPay == null ? WeChatVirtualPayProperties.empty() : wechatVirtualPay;
    }

    public record WeChatPayProperties(
            String appId,
            String merchantId,
            String merchantSerialNumber,
            String privateKeyPath,
            String apiV3Key,
            String notifyUrl) {

        public static WeChatPayProperties empty() {
            return new WeChatPayProperties("", "", "", "", "", "");
        }

        public boolean configured() {
            return hasText(appId)
                    && hasText(merchantId)
                    && hasText(merchantSerialNumber)
                    && hasText(privateKeyPath)
                    && hasText(apiV3Key)
                    && hasText(notifyUrl);
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }

    public record WeChatVirtualPayProperties(
            String appId,
            String merchantId,
            String offerId,
            String appKey,
            String sandboxAppKey,
            int env,
            String mode,
            String productId,
            String notifyUrl) {

        public static WeChatVirtualPayProperties empty() {
            return new WeChatVirtualPayProperties("", "", "", "", "", 0, "short_series_goods", "", "");
        }

        public boolean configured() {
            return hasText(appId)
                    && hasText(merchantId)
                    && hasText(offerId)
                    && hasText(appKeyForCurrentEnv())
                    && env >= 0
                    && env <= 1
                    && "short_series_goods".equals(mode)
                    && hasText(productId);
        }

        public String appKeyForCurrentEnv() {
            return env == 1 ? sandboxAppKey : appKey;
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
