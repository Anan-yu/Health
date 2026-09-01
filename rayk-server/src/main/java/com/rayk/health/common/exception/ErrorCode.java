package com.rayk.health.common.exception;

public enum ErrorCode {
    AUTH_INVALID_CREDENTIALS(10101, "用户名或密码错误"),
    AUTH_UNAUTHORIZED(10102, "登录状态无效或已过期"),
    AUTH_FORBIDDEN(10103, "无权执行此操作"),
    WECHAT_LOGIN_FAILED(10201, "微信登录凭证校验失败，请重试"),
    WECHAT_NOT_CONFIGURED(10202, "微信小程序身份服务尚未配置"),
    WECHAT_ACCOUNT_NOT_BOUND(10203, "该微信账号尚未绑定系统用户"),
    WECHAT_ALREADY_BOUND(10204, "该微信账号已绑定其他系统用户"),
    WECHAT_PHONE_AUTH_FAILED(10205, "微信手机号授权校验失败，请重试"),
    PHONE_ALREADY_REGISTERED(10206, "该手机号已被系统账户使用"),
    WORKBENCH_NOT_ALLOWED(10301, "当前账号无权切换到该工作台"),
    TENANT_NOT_FOUND(10401, "机构不存在或无权访问"),
    PLATFORM_CUSTOMER_NOT_FOUND(10402, "未找到对应的客户账号"),
    DATA_COLLECTION_CONSENT_REQUIRED(10501, "请先授权健康数据采集后再继续"),
    HEALTH_ASSESSMENT_CONSENT_REQUIRED(10502, "请先授权健康评估服务后再继续"),
    DATA_SHARING_CONSENT_REQUIRED(10503, "客户尚未授权专业人员协作"),
    PATIENT_NOT_FOUND(20101, "客户不存在或无权访问"),
    LAB_REPORT_NOT_FOUND(30101, "检验报告不存在或无权访问"),
    LAB_REPORT_INVALID_STATUS(30102, "检验报告当前状态不允许此操作"),
    FILE_INVALID(30201, "仅支持真实PDF、JPG或PNG文件"),
    FILE_NOT_FOUND(30202, "报告文件不存在或无权访问"),
    FILE_STORAGE_UNAVAILABLE(30203, "文件存储服务暂时不可用"),
    OCR_TASK_NOT_FOUND(40101, "报告识别任务不存在"),
    OCR_TASK_PROCESSING(40102, "报告正在识别，请稍后再试"),
    OCR_SERVICE_UNAVAILABLE(40103, "报告识别服务暂时不可用，可稍后重新识别"),
    AI_SERVICE_UNAVAILABLE(50101, "AI服务暂时不可用"),
    VOICE_SERVICE_UNAVAILABLE(50102, "语音服务暂时不可用，请稍后再试"),
    VOICE_SERVICE_QUOTA_EXHAUSTED(50103, "语音服务当前没有可用调用额度，请联系管理员开通或充值"),
    REVIEW_INVALID_STATUS(60101, "审核任务当前状态不允许此操作"),
    FOLLOWUP_NOT_FOUND(60201, "随访任务或计划不存在或无权访问"),
    FOLLOWUP_INVALID_STATUS(60202, "随访任务或计划当前状态不允许此操作"),
    HEALTH_SCAN_NOT_CONFIGURED(60301, "健康检测服务尚未完成配置"),
    HEALTH_SCAN_NOT_FOUND(60302, "健康检测任务不存在或无权访问"),
    HEALTH_SCAN_INVALID_STATUS(60303, "健康检测任务当前状态不允许此操作"),
    HEALTH_SCAN_SERVICE_UNAVAILABLE(60304, "健康检测服务暂时不可用，请稍后重试"),
    MEMBERSHIP_BENEFIT_NOT_AVAILABLE(60401, "当前会员权益不足或已过期，请升级会员后继续"),
    MEMBERSHIP_ORDER_NOT_FOUND(60402, "会员订单不存在或无权访问"),
    MEMBERSHIP_PAYMENT_NOT_CONFIGURED(60403, "微信支付尚未完成商户配置，请联系管理员"),
    MEMBERSHIP_PAYMENT_UNAVAILABLE(60404, "微信支付服务暂时不可用，请稍后重试"),
    WECHAT_PAYMENT_SESSION_EXPIRED(60405, "微信登录状态已失效，请重新进入小程序登录后再支付"),
    MEMBERSHIP_PAYMENT_AUTH_REQUIRED(60406, "微信支付提示：商户号该产品权限未开通，请在商户平台检查后重试"),
    MEDICAL_ASSISTANT_CONVERSATION_NOT_FOUND(60501, "对话不存在或无权访问"),
    MALL_NOT_ENABLED(60600, "商城暂未开放"),
    MALL_PRODUCT_NOT_FOUND(60601, "商品不存在或已下架"),
    MALL_ADDRESS_NOT_FOUND(60602, "收货地址不存在或无权访问"),
    MALL_ORDER_NOT_FOUND(60603, "商城订单不存在或无权访问"),
    MALL_STOCK_NOT_ENOUGH(60604, "商品库存不足"),
    MALL_ORDER_INVALID_STATUS(60605, "订单当前状态不允许此操作"),
    MALL_PAYMENT_NOT_CONFIGURED(60606, "商城微信支付尚未完成小程序关联或JSAPI开通，请联系管理员"),
    MALL_PAYMENT_UNAVAILABLE(60607, "商城支付服务暂时不可用，请稍后重试"),
    MALL_PAYMENT_AUTH_REQUIRED(60608, "商城微信支付提示：商户号该产品权限未开通，请在商户平台检查后重试"),
    GOLD_BEAN_NOT_ENABLED(60700, "金豆会员功能尚未在当前环境开启"),
    GOLD_BEAN_ACCOUNT_NOT_FOUND(60701, "金豆会员账户不存在或无权访问"),
    GOLD_BEAN_REFERRAL_INVALID(60702, "推荐码无效，不能建立推荐关系"),
    GOLD_BEAN_ALREADY_REGISTERED(60703, "当前账号已完成金豆会员注册"),
    GOLD_BEAN_LEVEL_REQUIRED(60704, "当前会员等级暂不具备该权限"),
    GOLD_BEAN_REGION_CONFLICT(60705, "该城市或账号已经开辟区域"),
    GOLD_BEAN_BALANCE_INSUFFICIENT(60706, "可交易金豆余额不足"),
    GOLD_BEAN_PROFIT_INVALID(60707, "区域分润金额或区域状态无效"),
    INDICATOR_NOT_FOUND(70101, "指标字典项不存在或无权访问"),
    INDICATOR_CODE_DUPLICATE(70102, "指标编码已存在"),
    INDICATOR_ALIAS_DUPLICATE(70103, "指标别名已存在"),
    MODEL_CONFIG_NOT_FOUND(80101, "评估模型配置不存在"),
    MODEL_CONFIG_INVALID_STATUS(80102, "评估模型配置当前状态不允许此操作"),
    SYSTEM_VALIDATION_ERROR(90001, "请求参数校验失败"),
    SYSTEM_ERROR(99999, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
