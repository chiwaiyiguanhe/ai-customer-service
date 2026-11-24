package com.ai.assistant.listener;

import com.ai.assistant.dto.AgentTrace;
import com.ai.assistant.dto.ToolCallRecord;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentTraceCollector 单元测试
 * 模拟 Listener 回调序列（两轮：模型请求调工具 → 工具结果回传 → 最终回答），验证轨迹重建逻辑
 */
class AgentTraceCollectorTest {

    @AfterEach
    void tearDown() {
        AgentTraceCollector.clear();
    }

    @Test
    void testCompleteTrace_TwoRounds() {
        AgentTraceCollector.start("trace-1", "张三买了什么？");

        // 第 1 轮：模型决定调用工具
        AgentTraceCollector.onRequest(List.of(
                SystemMessage.from("你是客服助手"),
                UserMessage.from("张三买了什么？")));
        AgentTraceCollector.onResponse(null,
                List.of(new ToolCallRecord("queryOrders", "{\"username\":\"张三\"}", "call-1")),
                100, 20);

        // 第 2 轮：上一轮的工具调用与结果出现在请求消息里
        AiMessage toolCallMessage = new AiMessage(List.of(ToolExecutionRequest.builder()
                .id("call-1").name("queryOrders").arguments("{\"username\":\"张三\"}").build()));
        AgentTraceCollector.onRequest(List.of(
                SystemMessage.from("你是客服助手"),
                UserMessage.from("张三买了什么？"),
                toolCallMessage,
                new ToolExecutionResultMessage("call-1", "queryOrders", "[{\"product\":\"iPhone 15\"}]")));
        AgentTraceCollector.onResponse("张三买了 iPhone 15。", List.of(), 150, 30);

        AgentTrace trace = AgentTraceCollector.complete("张三买了 iPhone 15。", 3500L);

        assertEquals("trace-1", trace.traceId());
        assertEquals("张三买了什么？", trace.question());
        assertEquals(2, trace.totalRounds());
        assertEquals(3500L, trace.totalDurationMs());
        assertEquals(250, trace.totalInputTokens());
        assertEquals(50, trace.totalOutputTokens());

        // 第 1 轮：模型请求调用工具，无文本
        assertEquals(1, trace.rounds().get(0).toolCalls().size());
        assertEquals("queryOrders", trace.rounds().get(0).toolCalls().get(0).name());
        assertNull(trace.rounds().get(0).modelText());

        // 第 2 轮：请求里能看到上一轮的工具调用与结果——闭环证据
        assertTrue(trace.rounds().get(1).requestMessages().stream()
                .anyMatch(p -> p.startsWith("AI→tool_calls: queryOrders")));
        assertTrue(trace.rounds().get(1).requestMessages().stream()
                .anyMatch(p -> p.startsWith("TOOL_RESULT[queryOrders]")));
        assertEquals("张三买了 iPhone 15。", trace.rounds().get(1).modelText());
        assertTrue(trace.rounds().get(1).toolCalls().isEmpty());

        assertEquals("张三买了 iPhone 15。", trace.finalAnswer());
        assertFalse(AgentTraceCollector.isActive());
    }

    @Test
    void testPreview_TruncatesLongContent() {
        List<String> previews = AgentTraceCollector.buildRequestPreviews(List.of(
                new ToolExecutionResultMessage("call-1", "queryOrders", "a".repeat(500))));
        assertEquals(1, previews.size());
        assertTrue(previews.get(0).startsWith("TOOL_RESULT[queryOrders]: "));
        assertTrue(previews.get(0).length() < 200);
        assertTrue(previews.get(0).endsWith("…"));
    }

    @Test
    void testClear_DiscardsState() {
        AgentTraceCollector.start("trace-2", "问题");
        AgentTraceCollector.clear();
        assertFalse(AgentTraceCollector.isActive());
    }
}
