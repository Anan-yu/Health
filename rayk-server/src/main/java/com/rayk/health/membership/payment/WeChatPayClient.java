package com.rayk.health.membership.payment;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.membership.config.MembershipProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * 微信支付 API v3 的最小封装。
 *
 * <p>客户端只在第一次真实支付或回调到达时初始化 SDK，避免开发环境因未配置商户证书而无法启动。
 */
@Service
public class WeChatPayClient {
    private final MembershipProperties properties;
    private volatile JsapiServiceExtension jsapiService;
    private volatile NotificationParser notificationParser;

    public WeChatPayClient(MembershipProperties properties) {
        this.properties = properties;
    }

    public boolean configured() {
        MembershipProperties.WeChatPayProperties pay = properties.wechatPay();
        return pay.configured() && readableFile(pay.privateKeyPath());
    }

    public PrepayWithRequestPaymentResponse prepay(PrepayRequest request) {
        try {
            return jsapiService().prepayWithRequestPayment(request);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        }
    }

    public Transaction parseNotification(RequestParam requestParam) {
        try {
            return notificationParser().parse(requestParam, Transaction.class);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // The callback controller maps invalid signatures/decryption to HTTP 401/400.
            throw exception;
        }
    }

    private JsapiServiceExtension jsapiService() {
        if (jsapiService == null) {
            initialize();
        }
        return jsapiService;
    }

    private NotificationParser notificationParser() {
        if (notificationParser == null) {
            initialize();
        }
        return notificationParser;
    }

    private synchronized void initialize() {
        if (jsapiService != null && notificationParser != null) {
            return;
        }
        if (!configured()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
        }
        MembershipProperties.WeChatPayProperties pay = properties.wechatPay();
        RSAAutoCertificateConfig built =
                new RSAAutoCertificateConfig.Builder()
                        .merchantId(pay.merchantId())
                        .privateKeyFromPath(pay.privateKeyPath())
                        .merchantSerialNumber(pay.merchantSerialNumber())
                        .apiV3Key(pay.apiV3Key())
                        .build();
        jsapiService = new JsapiServiceExtension.Builder().config(built).signType("RSA").build();
        notificationParser = new NotificationParser(built);
    }

    private boolean readableFile(String path) {
        return path != null && !path.isBlank() && Files.isReadable(Path.of(path));
    }
}
