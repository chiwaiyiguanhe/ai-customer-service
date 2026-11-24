package com.ai.assistant.dto;

import java.util.List;

/**
 * Agent 循环中的一轮（一次模型调用）
 *
 * requestMessages 里出现的 TOOL_RESULT 前缀消息，是"工具结果在下一轮请求中回传给模型"的直接证据；
 * toolCalls 非空表示模型本轮决定继续调用工具，modelText 非空表示模型本轮产出最终回答。
 */
public record TraceRound(
        int round,
        long durationMs,
        Integer inputTokens,
        Integer outputTokens,
        List<String> requestMessages,
        String modelText,
        List<ToolCallRecord> toolCalls,
        String error) {
}
