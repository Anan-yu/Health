package com.rayk.health.membership.payment;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.membership.config.MembershipProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.http.DefaultHttpClientBuilder;
import com.wechat.pay.java.core.http.HttpClient;
import com.wechat.pay.java.core.http.HttpMethod;
import com.wechat.pay.java.core.http.HttpRequest;
import com.wechat.pay.java.core.http.HttpResponse;
import com.wechat.pay.java.core.http.JsonRequestBody;
import com.wechat.pay.java.core.http.JsonResponseBody;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RSAPublicKeyNotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.transferbatch.TransferBatchService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 微信支付 API v3 的最小封装。
 *
 * <p>客户端只在第一次真实支付或回调到达时初始化 SDK，避免开发环境因未配置商户证书而无法启动。
 */
@Service
public class WeChatPayClient {
    private static final Logger log = LoggerFactory.getLogger(WeChatPayClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final MembershipProperties properties;
    private final ThreadLocal<WeChatResponseMetadata> responseMetadata = new ThreadLocal<>();
    private final ThreadLocal<Boolean> responseLogged = new ThreadLocal<>();
    private volatile JsapiServiceExtension jsapiService;
    private volatile NotificationParser notificationParser;
    private volatile TransferBatchService transferBatchService;
    private volatile HttpClient apiHttpClient;

    public WeChatPayClient(MembershipProperties properties) {
        this.properties = properties;
    }

    public boolean configured() {
        MembershipProperties.WeChatPayProperties pay = properties.wechatPay();
        if (!pay.configured() || !readableFile(pay.privateKeyPath())) {
            return false;
        }
        if (!pay.hasAnyPlatformPublicKeySetting()) {
            return true;
        }
        return pay.platformPublicKeyConfigured() && readableFile(pay.platformPublicKeyPath());
    }

    public PrepayWithRequestPaymentResponse prepay(PrepayRequest request) {
        responseMetadata.remove();
        responseLogged.remove();
        try {
            return jsapiService().prepayWithRequestPayment(request);
        } catch (BusinessException exception) {
            throw exception;
        } catch (ServiceException exception) {
            WeChatResponseMetadata metadata = responseMetadata.get();
            String errorCode = safeExternalValue(exception.getErrorCode());
            if (!Boolean.TRUE.equals(responseLogged.get())) {
                log.warn(
                        "WeChat JSAPI prepay response: requestUrl={}, httpStatus={}, code={}, message={}, wechatpayRequestId={}, apiPath={}",
                        metadata == null ? safeRequestUrl(exception) : metadata.requestUrl(),
                        exception.getHttpStatusCode(),
                        errorCode,
                        safeExternalMessage(exception.getErrorMessage()),
                        metadata == null ? "unavailable" : safeRequestId(metadata.wechatpayRequestId()),
                        metadata == null ? safeApiPath(exception) : metadata.apiPath());
            }
            if ("RESOURCE_NOT_EXISTS".equals(errorCode)) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
            }
            if ("NO_AUTH".equals(errorCode)) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_AUTH_REQUIRED);
            }
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        } catch (RuntimeException exception) {
            String errorCode = safeExceptionValue(exception, "getErrorCode");
            WeChatResponseMetadata metadata = responseMetadata.get();
            if (metadata == null) {
                log.warn(
                        "WeChat JSAPI prepay response: requestUrl={}, httpStatus={}, code={}, message={}, wechatpayRequestId={}, apiPath={}, exceptionType={}",
                        safeExceptionRequestUrl(exception),
                        safeExceptionValue(exception, "getHttpStatusCode"),
                        errorCode,
                        safeExternalMessage(safeExceptionTextValue(exception, "getErrorMessage")),
                        "unavailable",
                        safeExceptionApiPath(exception),
                        exception.getClass().getName());
            }
            if ("RESOURCE_NOT_EXISTS".equals(errorCode)) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
            }
            if ("NO_AUTH".equals(errorCode)) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_AUTH_REQUIRED);
            }
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        } finally {
            responseMetadata.remove();
            responseLogged.remove();
        }
    }

    public TransferBatchService transferBatch() {
        if (transferBatchService == null) {
            initialize();
        }
        return transferBatchService;
    }

    /**
     * Executes a signed raw API v3 JSON request with the same merchant credential and response
     * validator used by the typed SDK services. This is needed for newer WeChat endpoints that
     * are not yet exposed by the pinned SDK version.
     */
    public String executeJson(HttpMethod method, String path, String body) {
        if (method == null || path == null || !path.startsWith("/v3/")) {
            throw new IllegalArgumentException("Invalid WeChat API request");
        }
        try {
            HttpClient client = apiClient();
            HttpRequest.Builder request = new HttpRequest.Builder()
                    .httpMethod(method)
                    .url("https://api.mch.weixin.qq.com" + path)
                    .addHeader("Accept", "application/json");
            if (body != null) {
                request.addHeader("Content-Type", "application/json")
                        .body(new JsonRequestBody.Builder().body(body).build());
            }
            HttpResponse<JsonResponseBody> response = client.execute(request.build(), JsonResponseBody.class);
            // The SDK's serviceResponse is Gson-deserialized as JsonResponseBody. For a raw
            // WeChat response such as {"state":"WAIT_USER_CONFIRM",...}, that object has no
            // `body` property and therefore contains an empty body. The original response body
            // is the JsonResponseBody attached to HttpResponse#getBody().
            JsonResponseBody responseBody = response.getBody() instanceof JsonResponseBody json
                    ? json
                    : null;
            return responseBody == null ? "" : responseBody.getBody();
        } finally {
            responseMetadata.remove();
            responseLogged.remove();
        }
    }

    public MembershipProperties.WeChatPayProperties paymentProperties() {
        return properties.wechatPay();
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

    public <T> T parseNotification(RequestParam requestParam, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        try {
            return notificationParser().parse(requestParam, type);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
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

    private HttpClient apiClient() {
        if (apiHttpClient == null) {
            initialize();
        }
        return apiHttpClient;
    }

    private synchronized void initialize() {
        if (jsapiService != null
                && notificationParser != null
                && transferBatchService != null
                && apiHttpClient != null) {
            return;
        }
        if (!configured()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
        }
        MembershipProperties.WeChatPayProperties pay = properties.wechatPay();
        Config built;
        NotificationConfig notificationConfig;
        if (pay.platformPublicKeyConfigured()) {
            built = new RSAPublicKeyConfig.Builder()
                    .merchantId(pay.merchantId())
                    .privateKeyFromPath(pay.privateKeyPath())
                    .merchantSerialNumber(pay.merchantSerialNumber())
                    .apiV3Key(pay.apiV3Key())
                    .publicKeyFromPath(pay.platformPublicKeyPath())
                    .publicKeyId(pay.platformPublicKeyId())
                    .build();
            notificationConfig = new RSAPublicKeyNotificationConfig.Builder()
                    .publicKeyFromPath(pay.platformPublicKeyPath())
                    .publicKeyId(pay.platformPublicKeyId())
                    .apiV3Key(pay.apiV3Key())
                    .build();
        } else {
            RSAAutoCertificateConfig certificateConfig = new RSAAutoCertificateConfig.Builder()
                    .merchantId(pay.merchantId())
                    .privateKeyFromPath(pay.privateKeyPath())
                    .merchantSerialNumber(pay.merchantSerialNumber())
                    .apiV3Key(pay.apiV3Key())
                    .build();
            built = certificateConfig;
            notificationConfig = certificateConfig;
        }
        HttpClient builtHttpClient = httpClient(built);
        jsapiService = new JsapiServiceExtension.Builder()
                .config(built)
                .httpClient(builtHttpClient)
                .signType("RSA")
                .build();
        notificationParser = new NotificationParser(notificationConfig);
        transferBatchService = new TransferBatchService.Builder()
                .config(built)
                .httpClient(builtHttpClient)
                .build();
        apiHttpClient = builtHttpClient;
    }

    private HttpClient httpClient(Config config) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response response = chain.proceed(chain.request());
                    String requestUrl = chain.request().url().newBuilder().query(null).build().toString();
                    String apiPath = chain.request().url().encodedPath();
                    String requestId = response.header("Wechatpay-Request-Id", response.header("Request-ID", "unavailable"));
                    responseMetadata.set(new WeChatResponseMetadata(
                            requestUrl,
                            requestId,
                            apiPath));
                    if (isTrackedPaymentPath(apiPath)) {
                        logWechatResponse(requestUrl, response, requestId, apiPath);
                        responseLogged.set(true);
                    }
                    return response;
                })
                .build();
        return new DefaultHttpClientBuilder().config(config).okHttpClient(client).build();
    }

    private boolean readableFile(String path) {
        return path != null && !path.isBlank() && Files.isReadable(Path.of(path));
    }

    /**
     * Logs only safe machine-readable payment diagnostics. Exception messages may contain signed
     * requests, OpenID values, or merchant identifiers and must never enter application logs.
     */
    private String safeExceptionValue(RuntimeException exception, String methodName) {
        try {
            Method method = exception.getClass().getMethod(methodName);
            Object value = method.invoke(exception);
            if (value instanceof Number) {
                return value.toString();
            }
            if (value instanceof String text && text.matches("[A-Z0-9_-]{1,64}")) {
                return text;
            }
        } catch (ReflectiveOperationException ignored) {
            // The SDK exception may not expose this diagnostic; class name remains available.
        }
        return "unavailable";
    }

    private String safeExternalValue(String value) {
        return value != null && value.matches("[A-Z0-9_-]{1,64}") ? value : "unavailable";
    }

    private String safeExternalMessage(String value) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }
        String normalized = value.replaceAll("[\\r\\n\\t]", " ");
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512) + "…";
    }

    private String safeRequestId(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]{1,256}") ? value : "unavailable";
    }

    private void logWechatResponse(String requestUrl, Response response, String requestId, String apiPath) {
        String code = "unavailable";
        String message = "unavailable";
        try {
            JsonNode payload = OBJECT_MAPPER.readTree(response.peekBody(64 * 1024L).string());
            code = safeExternalValue(payload.path("code").asText(null));
            message = safeExternalMessage(payload.path("message").asText(null));
        } catch (Exception ignored) {
            // A non-JSON or unreadable response is still diagnosed by URL, status and request ID.
        }
        log.warn(
                "WeChat API response: requestUrl={}, httpStatus={}, code={}, message={}, wechatpayRequestId={}, apiPath={}",
                requestUrl,
                response.code(),
                code,
                message,
                safeRequestId(requestId),
                apiPath);
    }

    private boolean isJsapiPrepayPath(String apiPath) {
        return "/v3/pay/transactions/jsapi".equals(apiPath)
                || "/v3/pay/partner/transactions/jsapi".equals(apiPath);
    }

    private boolean isTrackedPaymentPath(String apiPath) {
        return isJsapiPrepayPath(apiPath)
                || "/v3/fund-app/mch-transfer/transfer-bills".equals(apiPath)
                || apiPath.startsWith("/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/")
                || "/v3/fund-app/mch-transfer/transfer-bills/transfer".equals(apiPath)
                || "/v3/fund-app/mch-transfer/user-confirm-authorization".equals(apiPath)
                || apiPath.startsWith("/v3/fund-app/mch-transfer/user-confirm-authorization/out-authorization-no/");
    }

    private String safeRequestUrl(ServiceException exception) {
        if (exception.getHttpRequest() == null || exception.getHttpRequest().getUrl() == null) {
            return "unavailable";
        }
        return exception.getHttpRequest().getUrl().getProtocol()
                + "://"
                + exception.getHttpRequest().getUrl().getHost()
                + exception.getHttpRequest().getUrl().getPath();
    }

    private String safeApiPath(ServiceException exception) {
        if (exception.getHttpRequest() == null || exception.getHttpRequest().getUri() == null) {
            return "unavailable";
        }
        return exception.getHttpRequest().getUri().getPath();
    }

    private String safeExceptionTextValue(RuntimeException exception, String methodName) {
        try {
            Method method = exception.getClass().getMethod(methodName);
            Object value = method.invoke(exception);
            return value instanceof String text ? text : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String safeExceptionRequestUrl(RuntimeException exception) {
        try {
            Method method = exception.getClass().getMethod("getHttpRequest");
            Object request = method.invoke(exception);
            if (request == null) {
                return "unavailable";
            }
            Object url = request.getClass().getMethod("getUrl").invoke(request);
            if (url == null) {
                return "unavailable";
            }
            return url.getClass().getMethod("getProtocol").invoke(url)
                    + "://"
                    + url.getClass().getMethod("getHost").invoke(url)
                    + url.getClass().getMethod("getPath").invoke(url);
        } catch (ReflectiveOperationException ignored) {
            return "unavailable";
        }
    }

    private String safeExceptionApiPath(RuntimeException exception) {
        try {
            Method method = exception.getClass().getMethod("getHttpRequest");
            Object request = method.invoke(exception);
            if (request == null) {
                return "unavailable";
            }
            Object uri = request.getClass().getMethod("getUri").invoke(request);
            Object path = uri == null ? null : uri.getClass().getMethod("getPath").invoke(uri);
            return path instanceof String value && isTrackedPaymentPath(value) ? value : "unavailable";
        } catch (ReflectiveOperationException ignored) {
            return "unavailable";
        }
    }

    private record WeChatResponseMetadata(String requestUrl, String wechatpayRequestId, String apiPath) {}
}
