package com.rayk.health.membership.payment;

/**
 * Decrypted payload sent by WeChat for a user-confirmed merchant-transfer authorization.
 * Field names intentionally follow WeChat's API-v3 payload so the SDK notification parser can
 * deserialize the object without exposing the encrypted callback body to application logs.
 */
public class WechatMerchantTransferAuthorizationNotification {
    public String out_authorization_no;
    public String appid;
    public String openid;
    public String authorization_id;
    public String state;
    public String authorize_time;
}
