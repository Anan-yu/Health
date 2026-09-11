package com.rayk.health.membership.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.membership.config.MembershipProperties;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.http.HttpMethod;
import org.junit.jupiter.api.Test;

class WeChatMerchantTransferClientTest {
    private static final String TRANSFER_PATH = "/v3/fund-app/mch-transfer/transfer-bills";
    private static final String AUTHORIZATION_PATH = "/v3/fund-app/mch-transfer/user-confirm-authorization";

    @Test
    void buildsTheUpgradedReferralTransferPayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        WeChatPayClient payClient = mock(WeChatPayClient.class);
        when(payClient.executeJson(eq(HttpMethod.POST), eq(TRANSFER_PATH), anyString()))
                .thenAnswer(invocation -> {
                    JsonNode request = objectMapper.readTree(invocation.getArgument(2, String.class));
                    assertThat(request.get("appid").asText()).isEqualTo("app-id");
                    assertThat(request.get("out_bill_no").asText()).isEqualTo("GBR123T");
                    assertThat(request.get("transfer_scene_id").asText()).isEqualTo("1005");
                    assertThat(request.get("openid").asText()).isEqualTo("openid");
                    assertThat(request.get("transfer_amount").asLong()).isEqualTo(100L);
                    assertThat(request.get("user_recv_perception").asText()).isEqualTo("劳务报酬");
                    assertThat(request.get("transfer_scene_report_infos")).hasSize(2);
                    assertThat(request.get("transfer_scene_report_infos").get(0).get("info_type").asText())
                            .isEqualTo("岗位类型");
                    assertThat(request.get("transfer_scene_report_infos").get(1).get("info_type").asText())
                            .isEqualTo("报酬说明");
                    return "{\"transfer_bill_no\":\"MCH123\",\"state\":\"WAIT_USER_CONFIRM\",\"package_info\":\"confirm-package\"}";
                });

        WeChatMerchantTransferClient client = new WeChatMerchantTransferClient(
                properties(), payClient, objectMapper);

        WeChatMerchantTransferClient.TransferBill bill = client.initiate(
                "GBR123T",
                "openid",
                100,
                "1005",
                "https://example.test/notify",
                "劳务报酬",
                "推荐注册服务",
                "推荐注册服务报酬");

        assertThat(bill.transferBillNo()).isEqualTo("MCH123");
        assertThat(bill.state()).isEqualTo("WAIT_USER_CONFIRM");
        assertThat(bill.packageInfo()).isEqualTo("confirm-package");
    }

    @Test
    void queriesByThePersistedExternalBillNumber() {
        WeChatPayClient payClient = mock(WeChatPayClient.class);
        when(payClient.executeJson(
                        eq(HttpMethod.GET),
                        eq(TRANSFER_PATH + "/out-bill-no/GBR123T"),
                        isNull()))
                .thenReturn("{\"transfer_bill_no\":\"MCH123\",\"state\":\"SUCCESS\"}");

        WeChatMerchantTransferClient client = new WeChatMerchantTransferClient(
                properties(), payClient, new ObjectMapper());

        WeChatMerchantTransferClient.TransferBill bill = client.query("GBR123T");

        assertThat(bill.transferBillNo()).isEqualTo("MCH123");
        assertThat(bill.state()).isEqualTo("SUCCESS");
        verify(payClient).executeJson(
                HttpMethod.GET,
                TRANSFER_PATH + "/out-bill-no/GBR123T",
                null);
    }

    @Test
    void exposesNotFoundSoTheSameBillNumberCanBeRetried() {
        WeChatPayClient payClient = mock(WeChatPayClient.class);
        ServiceException notFound = mock(ServiceException.class);
        when(notFound.getErrorCode()).thenReturn("NOT_FOUND");
        when(payClient.executeJson(
                        eq(HttpMethod.GET),
                        eq(TRANSFER_PATH + "/out-bill-no/GBR123T"),
                        isNull()))
                .thenThrow(notFound);

        WeChatMerchantTransferClient client = new WeChatMerchantTransferClient(
                properties(), payClient, new ObjectMapper());

        assertThatThrownBy(() -> client.query("GBR123T"))
                .isInstanceOf(WeChatMerchantTransferClient.TransferBillNotFoundException.class)
                .hasMessageContaining("GBR123T");
    }

    @Test
    void buildsTheOneTimeReceiveAuthorizationPayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        WeChatPayClient payClient = mock(WeChatPayClient.class);
        when(payClient.executeJson(eq(HttpMethod.POST), eq(AUTHORIZATION_PATH), anyString()))
                .thenAnswer(invocation -> {
                    JsonNode request = objectMapper.readTree(invocation.getArgument(2, String.class));
                    assertThat(request.get("out_authorization_no").asText()).isEqualTo("GRA123");
                    assertThat(request.get("appid").asText()).isEqualTo("app-id");
                    assertThat(request.get("openid").asText()).isEqualTo("openid");
                    assertThat(request.get("transfer_scene_id").asText()).isEqualTo("1005");
                    assertThat(request.get("user_display_name").asText()).isEqualTo("三羊健康会员");
                    assertThat(request.get("authorization_notify_url").asText())
                            .isEqualTo("https://example.test/authorization-notify");
                    return "{\"out_authorization_no\":\"GRA123\",\"state\":\"WAIT_USER_CONFIRM\",\"package_info\":\"auth-package\"}";
                });

        WeChatMerchantTransferClient client = new WeChatMerchantTransferClient(
                properties(), payClient, objectMapper);

        WeChatMerchantTransferClient.TransferAuthorization authorization = client.initiateAuthorization(
                "GRA123",
                "openid",
                "1005",
                "三羊健康会员",
                "劳务报酬",
                "https://example.test/authorization-notify");

        assertThat(authorization.outAuthorizationNo()).isEqualTo("GRA123");
        assertThat(authorization.state()).isEqualTo("WAIT_USER_CONFIRM");
        assertThat(authorization.packageInfo()).isEqualTo("auth-package");
    }

    @Test
    void queriesAuthorizationAndTransfersWithTheAuthorizationId() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        WeChatPayClient payClient = mock(WeChatPayClient.class);
        when(payClient.executeJson(
                        eq(HttpMethod.GET),
                        eq(AUTHORIZATION_PATH + "/out-authorization-no/GRA123"),
                        isNull()))
                .thenReturn("{\"out_authorization_no\":\"GRA123\",\"authorization_id\":\"AUTH123\",\"state\":\"TAKING_EFFECT\"}");
        when(payClient.executeJson(
                        eq(HttpMethod.POST),
                        eq(TRANSFER_PATH + "/transfer"),
                        anyString()))
                .thenAnswer(invocation -> {
                    JsonNode request = objectMapper.readTree(invocation.getArgument(2, String.class));
                    assertThat(request.get("appid").asText()).isEqualTo("app-id");
                    assertThat(request.get("out_bill_no").asText()).isEqualTo("GBR123T");
                    assertThat(request.get("authorization_id").asText()).isEqualTo("AUTH123");
                    assertThat(request.get("transfer_amount").asLong()).isEqualTo(100L);
                    assertThat(request.get("openid")).isNull();
                    return "{\"transfer_bill_no\":\"MCH123\",\"state\":\"SUCCESS\"}";
                });

        WeChatMerchantTransferClient client = new WeChatMerchantTransferClient(
                properties(), payClient, objectMapper);

        WeChatMerchantTransferClient.TransferAuthorization authorization = client.queryAuthorization("GRA123");
        WeChatMerchantTransferClient.TransferBill bill = client.transferAfterAuthorization(
                "GBR123T",
                authorization.authorizationId(),
                100,
                "1005",
                "https://example.test/notify",
                "劳务报酬",
                "推荐注册服务",
                "推荐注册服务报酬");

        assertThat(authorization.state()).isEqualTo("TAKING_EFFECT");
        assertThat(bill.state()).isEqualTo("SUCCESS");
        verify(payClient).executeJson(
                HttpMethod.GET,
                AUTHORIZATION_PATH + "/out-authorization-no/GRA123",
                null);
    }

    @Test
    void rejectsNonHttpsAuthorizationCallbackUrl() {
        WeChatMerchantTransferClient client = new WeChatMerchantTransferClient(
                properties(), mock(WeChatPayClient.class), new ObjectMapper());

        assertThatThrownBy(() -> client.initiateAuthorization(
                        "GRA123",
                        "openid",
                        "1005",
                        "三羊健康会员",
                        "劳务报酬",
                        "http://example.test/notify"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    private MembershipProperties properties() {
        return new MembershipProperties(
                true,
                true,
                3,
                3,
                3,
                3,
                3,
                3,
                false,
                new MembershipProperties.WeChatPayProperties(
                        "app-id", "merchant-id", "serial", "private-key", "api-v3", "", "", "notify"),
                MembershipProperties.WeChatVirtualPayProperties.empty());
    }
}
