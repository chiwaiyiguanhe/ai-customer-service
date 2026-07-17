package com.ai.assistant.common;

/**
 * 自定义业务异常
 * 用于在业务逻辑中抛出可预期的错误（如：订单不存在、参数非法等）
 * 区别于系统异常（如 NullPointerException、数据库连接失败）
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
