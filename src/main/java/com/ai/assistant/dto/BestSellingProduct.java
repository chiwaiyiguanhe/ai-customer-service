package com.ai.assistant.dto;

/**
 * 畅销商品统计（工具返回给 LLM 的结构化数据）
 * product 为 null 表示暂无销售数据，由模型据此组织回答
 */
public record BestSellingProduct(String product, long salesCount) {
}
