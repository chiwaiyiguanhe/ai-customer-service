package com.ai.assistant.dto;

import java.util.List;

/**
 * 时间段订单查询结果（工具返回给 LLM 的结构化数据）
 *
 * 参数正常时 error 为 null，携带 orders；
 * 参数无法解析时 orders 为空列表，error 携带引导模型修正参数的错误说明。
 * 模型读到 error 后会自行修正入参重新调用，形成"调用失败 → 错误回传 → 自愈重试"闭环。
 * 错误说明也是数据——这就是把错误结构化、而不是抛异常炸掉会话的原因。
 */
public record OrdersByDateResult(List<OrderSummary> orders, String error) {

    public static OrdersByDateResult ok(List<OrderSummary> orders) {
        return new OrdersByDateResult(orders, null);
    }

    public static OrdersByDateResult fail(String message) {
        return new OrdersByDateResult(List.of(), message);
    }
}
