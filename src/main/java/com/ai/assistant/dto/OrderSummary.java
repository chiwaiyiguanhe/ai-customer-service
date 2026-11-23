package com.ai.assistant.dto;

import java.math.BigDecimal;

/**
 * 订单摘要（工具返回给 LLM 的结构化数据）
 * 只携带模型推理所需的原始数据字段，不拼接任何"客服话术"
 * orderTime 在工具层格式化为标准字符串，保证 JSON 序列化结果稳定可预期
 */
public record OrderSummary(String product, BigDecimal amount, String orderTime) {
}
