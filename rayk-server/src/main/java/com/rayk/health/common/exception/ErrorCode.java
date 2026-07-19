package com.rayk.health.common.exception;

public enum ErrorCode {
    AUTH_INVALID_CREDENTIALS(10101, "用户名或密码错误"),
    AUTH_UNAUTHORIZED(10102, "登录状态无效或已过期"),
    AUTH_FORBIDDEN(10103, "无权执行此操作"),
    WECHAT_LOGIN_FAILED(10201, "微信登录凭证校验失败，请重试"),
    WECHAT_NOT_CONFIGURED(10202, "微信小程序身份服务尚未配置"),
    WECHAT_ACCOUNT_NOT_BOUND(10203, "该微信账号尚未绑定系统用户"),
    WECHAT_ALREADY_BOUND(10204, "该微信账号已绑定其他系统用户"),
    WORKBENCH_NOT_ALLOWED(10301, "当前账号无权切换到该工作台"),
    PATIENT_NOT_FOUND(20101, "客户不存在或无权访问"),
    LAB_REPORT_NOT_FOUND(30101, "检验报告不存在或无权访问"),
    LAB_REPORT_INVALID_STATUS(30102, "检验报告当前状态不允许此操作"),
    FILE_INVALID(30201, "仅支持20MB以内的真实PDF、JPG或PNG文件"),
    FILE_NOT_FOUND(30202, "报告文件不存在或无权访问"),
    FILE_STORAGE_UNAVAILABLE(30203, "文件存储服务暂时不可用"),
    AI_SERVICE_UNAVAILABLE(50101, "AI服务暂时不可用"),
    REVIEW_INVALID_STATUS(60101, "审核任务当前状态不允许此操作"),
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
