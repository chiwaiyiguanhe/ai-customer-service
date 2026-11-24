package com.ai.assistant.service;

import com.ai.assistant.dto.AgentTrace;
import com.ai.assistant.listener.AgentTraceCollector;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 带执行链路追踪的对话编排服务
 * 职责：为一次对话创建采集上下文（traceId），执行同步对话，组装完整轨迹。
 * finally 里强制清理 ThreadLocal——线程池会复用线程，不清理会把上一条请求的轨迹串给下一条。
 */
@Service
public class TraceChatService {

    private final Assistant assistant;

    public TraceChatService(Assistant assistant) {
        this.assistant = assistant;
    }

    public AgentTrace chatWithTrace(String message) {
        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        AgentTraceCollector.start(traceId, message);
        long startNanos = System.nanoTime();
        try {
            String answer = assistant.chat(message);
            long totalDurationMs = (System.nanoTime() - startNanos) / 1_000_000;
            return AgentTraceCollector.complete(answer, totalDurationMs);
        } finally {
            AgentTraceCollector.clear();
        }
    }
}
