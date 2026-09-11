package com.rayk.health.membership.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.membership.config.MembershipProperties;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.http.HttpMethod;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Client for the upgraded WeChat merchant-transfer API. The pinned WeChat Java SDK still exposes
 * the legacy transfer-batch endpoints, so this client uses the SDK's signed raw API-v3 transport
 * while retaining response signature validation.
 */
@Service
public class WeChatMerchantTransferClient {
    private static final String TRANSFER_BILLS_PATH = "/v3/fund-app/mch-transfer/transfer-bills";
    private static final String TRANSFER_BILL_QUERY_PREFIX = TRANSFER_BILLS_PATH + "/out-bill-no/";
    private static final String AUTHORIZATION_PATH = "/v3/fund-app/mch-transfer/user-confirm-authorization";
    private static final String AUTHORIZATION_QUERY_PREFIX = AUTHORIZATION_PATH + "/out-authorization-no/";
    private static final String AUTHORIZED_TRANSFER_PATH = TRANSFER_BILLS_PATH + "/transfer";

    private final MembershipProperties properties;
    private final WeChatPayClient weChatPayClient;
    private final ObjectMapper objectMapper;

    public WeChatMerchantTransferClient(
            MembershipProperties properties,
            WeChatPayClient weChatPayClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.weChatPayClient = weChatPayClient;
        this.objectMapper = objectMapper;
    }

    public TransferBill initiate(
            String outBillNo,
            String openid,
            long amountCent,
            String sceneId,
            String notifyUrl,
            String userRecvPerception,
            String jobType,
            String rewardDescription) {
        return initiate(
                outBillNo,
                openid,
                amountCent,
                sceneId,
                notifyUrl,
                userRecvPerception,
                jobType,
                rewardDescription,
                "三羊健康推荐注册费");
    }

    /** Starts a transfer with a business-specific remark while retaining the same idempotency key. */
    public TransferBill initiate(
            String outBillNo,
            String openid,
            long amountCent,
            String sceneId,
            String notifyUrl,
            String userRecvPerception,
            String jobType,
            String rewardDescription,
            String transferRemark) {
        requireIdentifier(outBillNo);
        requireText(openid);
        requireText(sceneId);
        if (amountCent <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("appid", properties.wechatPay().appId());
        request.put("out_bill_no", outBillNo);
        request.put("transfer_scene_id", sceneId);
        request.put("openid", openid);
        request.put("transfer_amount", amountCent);
        request.put("transfer_remark", requireText(transferRemark));
        request.put("user_recv_perception", requireText(userRecvPerception));
        request.put(
                "transfer_scene_report_infos",
                List.of(
                        reportInfo("岗位类型", jobType),
                        reportInfo("报酬说明", rewardDescription)));
        if (StringUtils.hasText(notifyUrl)) {
            request.put("notify_url", notifyUrl.trim());
        }
        try {
            String body = objectMapper.writeValueAsString(request);
            return parse(weChatPayClient.executeJson(HttpMethod.POST, TRANSFER_BILLS_PATH, body));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode merchant transfer request", exception);
        }
    }

    public TransferBill query(String outBillNo) {
        requireIdentifier(outBillNo);
        try {
            return parse(weChatPayClient.executeJson(
                    HttpMethod.GET,
                    TRANSFER_BILL_QUERY_PREFIX + outBillNo,
                    null));
        } catch (ServiceException exception) {
            if ("NOT_FOUND".equalsIgnoreCase(exception.getErrorCode())) {
                throw new TransferBillNotFoundException(outBillNo);
            }
            throw exception;
        }
    }

    /**
     * Starts the one-time user-confirmation authorization flow. The returned package_info must be
     * opened in the mini program with wx.requestMerchantTransfer. Once the authorization is active,
     * future transfers can use transferAfterAuthorization without another user confirmation.
     */
    public TransferAuthorization initiateAuthorization(
            String outAuthorizationNo,
            String openid,
            String sceneId,
            String userDisplayName,
            String userRecvPerception,
            String authorizationNotifyUrl) {
        requireIdentifier(outAuthorizationNo);
        requireText(openid);
        requireText(sceneId);
        requireText(userDisplayName);
        requireText(userRecvPerception);
        requireHttpsCallbackUrl(authorizationNotifyUrl);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("out_authorization_no", outAuthorizationNo);
        request.put("appid", properties.wechatPay().appId());
        request.put("openid", openid);
        request.put("transfer_scene_id", sceneId);
        request.put("user_display_name", userDisplayName);
        request.put("user_recv_perception", userRecvPerception);
        request.put("authorization_notify_url", authorizationNotifyUrl.trim());
        try {
            String body = objectMapper.writeValueAsString(request);
            return parseAuthorization(weChatPayClient.executeJson(HttpMethod.POST, AUTHORIZATION_PATH, body));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode merchant transfer authorization request", exception);
        }
    }

    public TransferAuthorization queryAuthorization(String outAuthorizationNo) {
        requireIdentifier(outAuthorizationNo);
        try {
            return parseAuthorization(weChatPayClient.executeJson(
                    HttpMethod.GET,
                    AUTHORIZATION_QUERY_PREFIX + outAuthorizationNo,
                    null));
        } catch (ServiceException exception) {
            if ("NOT_FOUND".equalsIgnoreCase(exception.getErrorCode())) {
                throw new TransferAuthorizationNotFoundException(outAuthorizationNo);
            }
            throw exception;
        }
    }

    /** Creates a transfer that uses a previously activated authorization. */
    public TransferBill transferAfterAuthorization(
            String outBillNo,
            String authorizationId,
            long amountCent,
            String sceneId,
            String notifyUrl,
            String userRecvPerception,
            String jobType,
            String rewardDescription) {
        return transferAfterAuthorization(
                outBillNo,
                authorizationId,
                amountCent,
                sceneId,
                notifyUrl,
                userRecvPerception,
                jobType,
                rewardDescription,
                "三羊健康推荐注册费");
    }

    /** Starts an authorized transfer with a business-specific remark. */
    public TransferBill transferAfterAuthorization(
            String outBillNo,
            String authorizationId,
            long amountCent,
            String sceneId,
            String notifyUrl,
            String userRecvPerception,
            String jobType,
            String rewardDescription,
            String transferRemark) {
        requireIdentifier(outBillNo);
        requireText(authorizationId);
        requireText(sceneId);
        if (amountCent <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("appid", properties.wechatPay().appId());
        request.put("out_bill_no", outBillNo);
        request.put("transfer_scene_id", sceneId);
        request.put("authorization_id", authorizationId);
        request.put("transfer_amount", amountCent);
        request.put("transfer_remark", requireText(transferRemark));
        request.put("user_recv_perception", requireText(userRecvPerception));
        request.put(
                "transfer_scene_report_infos",
                List.of(
                        reportInfo("岗位类型", jobType),
                        reportInfo("报酬说明", rewardDescription)));
        if (StringUtils.hasText(notifyUrl)) {
            request.put("notify_url", notifyUrl.trim());
        }
        try {
            String body = objectMapper.writeValueAsString(request);
            return parse(weChatPayClient.executeJson(HttpMethod.POST, AUTHORIZED_TRANSFER_PATH, body));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode authorized merchant transfer request", exception);
        }
    }

    private Map<String, String> reportInfo(String type, String content) {
        return Map.of("info_type", type, "info_content", requireText(content));
    }

    private void requireHttpsCallbackUrl(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing merchant transfer authorization callback URL");
        }
        try {
            java.net.URI uri = java.net.URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !StringUtils.hasText(uri.getHost())
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Authorization callback URL must be HTTPS without query or fragment");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Authorization callback URL must be HTTPS without query or fragment", exception);
        }
    }

    private TransferBill parse(String body) {
        if (!StringUtils.hasText(body)) {
            return new TransferBill("", "", "", "");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return new TransferBill(
                    text(root, "transfer_bill_no", "transferBillNo"),
                    text(root, "state", "status"),
                    text(root, "fail_reason", "failReason"),
                    textOrJson(root, "package_info", "packageInfo"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid merchant transfer response", exception);
        }
    }

    private TransferAuthorization parseAuthorization(String body) {
        if (!StringUtils.hasText(body)) {
            return new TransferAuthorization("", "", "", "", "");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return new TransferAuthorization(
                    text(root, "out_authorization_no", "outAuthorizationNo"),
                    text(root, "authorization_id", "authorizationId"),
                    text(root, "state", "status"),
                    textOrJson(root, "package_info", "packageInfo"),
                    text(root, "fail_reason", "failReason"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid merchant transfer authorization response", exception);
        }
    }

    private String textOrJson(JsonNode node, String... names) {
        if (node == null) return "";
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) continue;
            if (value.isTextual() && StringUtils.hasText(value.textValue())) {
                return value.textValue().trim();
            }
            if (value.isContainerNode()) {
                return value.toString();
            }
        }
        return "";
    }

    private String text(JsonNode node, String... names) {
        if (node == null) return "";
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private String requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing merchant transfer parameter");
        }
        return value.trim();
    }

    private void requireIdentifier(String value) {
        if (!StringUtils.hasText(value) || !value.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("Invalid merchant transfer bill number");
        }
    }

    public record TransferBill(String transferBillNo, String state, String failReason, String packageInfo) {}

    public record TransferAuthorization(
            String outAuthorizationNo,
            String authorizationId,
            String state,
            String packageInfo,
            String failReason) {}

    /** The create request did not leave a retrievable bill, so the same bill number is safe to retry. */
    public static final class TransferBillNotFoundException extends RuntimeException {
        public TransferBillNotFoundException(String outBillNo) {
            super("Transfer bill not found: " + outBillNo);
        }
    }

    public static final class TransferAuthorizationNotFoundException extends RuntimeException {
        public TransferAuthorizationNotFoundException(String outAuthorizationNo) {
            super("Transfer authorization not found: " + outAuthorizationNo);
        }
    }
}
