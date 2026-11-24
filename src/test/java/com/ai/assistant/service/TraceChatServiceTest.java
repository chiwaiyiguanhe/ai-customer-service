package com.ai.assistant.service;

import com.ai.assistant.dto.AgentTrace;
import com.ai.assistant.listener.AgentTraceCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TraceChatService 单元测试
 * Mock 掉 AiService（单元测试不真调模型），验证编排逻辑：
 * 轨迹组装、finalAnswer 透传、成功与异常路径都必须清理 ThreadLocal（防线程池复用串数据）
 */
@ExtendWith(MockitoExtension.class)
class TraceChatServiceTest {

    @Mock
    private Assistant assistant;

    @InjectMocks
    private TraceChatService traceChatService;

    @Test
    void testChatWithTrace_AssemblesTraceAndClearsContext() {
        when(assistant.chat("你好")).thenReturn("你好呀！");

        AgentTrace trace = traceChatService.chatWithTrace("你好");

        assertEquals("你好呀！", trace.finalAnswer());
        assertEquals("你好", trace.question());
        assertNotNull(trace.traceId());
        assertTrue(trace.traceId().startsWith("trace-"));
        // 单元测试 Mock 了模型，Listener 不会真实触发，轮次为 0；轮次重建逻辑在 AgentTraceCollectorTest 覆盖
        assertEquals(0, trace.totalRounds());
        assertTrue(trace.totalDurationMs() >= 0);
        assertFalse(AgentTraceCollector.isActive());
        verify(assistant, times(1)).chat("你好");
    }

    @Test
    void testChatWithTrace_ExceptionStillClearsContext() {
        when(assistant.chat("爆炸")).thenThrow(new RuntimeException("模型超时"));

        assertThrows(RuntimeException.class, () -> traceChatService.chatWithTrace("爆炸"));

        // 异常路径同样必须清理，否则线程复用会把本次上下文串给下一条请求
        assertFalse(AgentTraceCollector.isActive());
    }
}
