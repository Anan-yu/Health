package com.rayk.health.membership.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wechat.pay.java.core.http.HttpClient;
import com.wechat.pay.java.core.http.HttpMethod;
import com.wechat.pay.java.core.http.HttpRequest;
import com.wechat.pay.java.core.http.HttpResponse;
import com.wechat.pay.java.core.http.JsonResponseBody;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WeChatPayClientTest {
    @Test
    void readsTheOriginalRawBodyForJsonApiResponses() throws Exception {
        String rawBody = "{\"transfer_bill_no\":\"MCH123\",\"state\":\"WAIT_USER_CONFIRM\"}";
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<JsonResponseBody> response = mock(HttpResponse.class);
        when(response.getBody()).thenReturn(jsonResponseBody(rawBody));
        when(httpClient.execute(any(HttpRequest.class), eq(JsonResponseBody.class))).thenReturn(response);

        WeChatPayClient client = new WeChatPayClient(null);
        ReflectionTestUtils.setField(client, "apiHttpClient", httpClient);

        assertThat(client.executeJson(
                        HttpMethod.GET,
                        "/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/GBR123T",
                        null))
                .isEqualTo(rawBody);
    }

    private JsonResponseBody jsonResponseBody(String body) throws Exception {
        Constructor<JsonResponseBody> constructor = JsonResponseBody.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(body);
    }
}
