package com.ai.assistant.dto;

import java.math.BigDecimal;

/**
 * 用户消费统计（工具返回给 LLM 的结构化数据）
 * totalAmount 为 0 表示无消费记录，由模型据此组织回答
 */
public record TotalSpent(String username, BigDecimal totalAmount, long orderCount) {
}
