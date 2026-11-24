package com.ai.assistant.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 执行链路轨迹（一次对话的完整决策记录）
 *
 * 把"黑盒循环"变成可观测、可复盘的白盒：模型几轮往返、每轮选了哪个工具、传了什么参数、
 * 工具结果如何回传、是否继续迭代、每步耗时与 token 用量。
 * 同时是性能账本——从轮次与耗时数据可以直接看出"瓶颈在模型轮次而非数据库"。
 */
public record AgentTrace(
        String traceId,
        String question,
        LocalDateTime startedAt,
        int totalRounds,
        long totalDurationMs,
        Integer totalInputTokens,
        Integer totalOutputTokens,
        List<TraceRound> rounds,
        String finalAnswer) {
}
