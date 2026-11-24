package com.ai.assistant.listener;

import com.ai.assistant.dto.ToolCallRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * LLM 模型监听器
 * 监听每一次模型调用（Agent 循环的每一轮）的请求、响应和错误
 * - 日志记录：保留原有行为
 * - 链路采集：把每轮数据喂给 AgentTraceCollector，供 /api/chat/trace 展示完整决策轨迹
 * 注意：采集以 ThreadLocal 为准——同步接口线程内可采集；流式接口在 Reactor 线程上无上下文，自动跳过
 */
public class ChatModelListenerImpl implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(ChatModelListenerImpl.class);

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("[LLM Request] {}", requestContext.chatRequest());
        if (AgentTraceCollector.isActive()) {
            List<ChatMessage> messages = requestContext.chatRequest().messages();
            AgentTraceCollector.onRequest(messages);
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ChatResponse response = responseContext.chatResponse();
        log.info("[LLM Response] {}", response);
        if (AgentTraceCollector.isActive()) {
            AiMessage aiMessage = response.aiMessage();
            List<ToolCallRecord> toolCalls = aiMessage == null || !aiMessage.hasToolExecutionRequests()
                    ? List.of()
                    : aiMessage.toolExecutionRequests().stream()
                        .map(r -> new ToolCallRecord(r.name(), r.arguments(), r.id()))
                        .toList();
            TokenUsage usage = response.tokenUsage();
            AgentTraceCollector.onResponse(
                    aiMessage == null ? null : aiMessage.text(),
                    toolCalls,
                    usage == null ? null : usage.inputTokenCount(),
                    usage == null ? null : usage.outputTokenCount());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.error("[LLM Error] {}", errorContext.error().getMessage(), errorContext.error());
        if (AgentTraceCollector.isActive()) {
            AgentTraceCollector.onError(errorContext.error().getMessage());
        }
    }
}
