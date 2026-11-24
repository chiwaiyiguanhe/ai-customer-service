package com.ai.assistant.listener;

import com.ai.assistant.dto.AgentTrace;
import com.ai.assistant.dto.ToolCallRecord;
import com.ai.assistant.dto.TraceRound;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行链路采集器
 *
 * 原理：LangChain4j 的 ChatModelListener 在每一次模型调用（即 Agent 循环的每一轮）触发一次
 * onRequest/onResponse；工具执行结果会作为 ToolExecutionResultMessage 出现在"下一轮"请求的
 * 消息列表里。因此逐轮记录请求/响应，即可重建完整决策链路：
 * 意图 → 工具选择 → 参数 → 结果 → 是否继续迭代 → 最终答案。
 *
 * 关联方式：同步接口下整次对话在同一个线程内完成（模型调用+工具执行），用 ThreadLocal 把
 * Listener 回调与本次请求绑定；流式接口跑在 Reactor 线程上取不到 ThreadLocal，Listener
 * 检测到无上下文时自动跳过（流式链路追踪是后续扩展点）。
 */
public final class AgentTraceCollector {

    private static final int PREVIEW_MAX_LENGTH = 160;

    private static final ThreadLocal<TraceState> STATE = new ThreadLocal<>();

    private AgentTraceCollector() {
    }

    /** 开始采集（由 /api/chat/trace 入口调用） */
    public static void start(String traceId, String question) {
        STATE.set(new TraceState(traceId, question));
    }

    /** 当前线程是否正在采集（Listener 回调据此决定是否记录，流式线程上为 false） */
    public static boolean isActive() {
        return STATE.get() != null;
    }

    /** Listener 每轮 onRequest 调用：开启本轮，记录请求消息预览 */
    public static void onRequest(List<ChatMessage> messages) {
        TraceState state = STATE.get();
        if (state == null) {
            return;
        }
        state.pendingStartNanos = System.nanoTime();
        state.pendingRequestMessages = buildRequestPreviews(messages);
    }

    /** Listener 每轮 onResponse 调用：闭合本轮，记录响应内容/工具调用/token 用量 */
    public static void onResponse(String modelText, List<ToolCallRecord> toolCalls,
                                  Integer inputTokens, Integer outputTokens) {
        TraceState state = STATE.get();
        if (state == null || state.pendingRequestMessages == null) {
            return;
        }
        long durationMs = (System.nanoTime() - state.pendingStartNanos) / 1_000_000;
        state.rounds.add(new TraceRound(
                state.rounds.size() + 1,
                durationMs,
                inputTokens,
                outputTokens,
                state.pendingRequestMessages,
                modelText,
                toolCalls == null ? List.of() : List.copyOf(toolCalls),
                null));
        state.pendingRequestMessages = null;
        if (inputTokens != null) {
            state.totalInputTokens = sum(state.totalInputTokens, inputTokens);
        }
        if (outputTokens != null) {
            state.totalOutputTokens = sum(state.totalOutputTokens, outputTokens);
        }
    }

    /** Listener onError 调用：闭合本轮，标记失败原因 */
    public static void onError(String message) {
        TraceState state = STATE.get();
        if (state == null || state.pendingRequestMessages == null) {
            return;
        }
        long durationMs = (System.nanoTime() - state.pendingStartNanos) / 1_000_000;
        state.rounds.add(new TraceRound(
                state.rounds.size() + 1,
                durationMs,
                null,
                null,
                state.pendingRequestMessages,
                null,
                List.of(),
                message));
        state.pendingRequestMessages = null;
    }

    /** 组装最终轨迹并结束采集 */
    public static AgentTrace complete(String finalAnswer, long totalDurationMs) {
        TraceState state = STATE.get();
        if (state == null) {
            return new AgentTrace("unknown", "", LocalDateTime.now(), 0, totalDurationMs,
                    null, null, List.of(), finalAnswer);
        }
        AgentTrace trace = new AgentTrace(
                state.traceId,
                state.question,
                state.startedAt,
                state.rounds.size(),
                totalDurationMs,
                state.totalInputTokens,
                state.totalOutputTokens,
                List.copyOf(state.rounds),
                finalAnswer);
        STATE.remove();
        return trace;
    }

    /** 清理线程上下文（异常路径兜底，防止线程池复用线程导致数据串话） */
    public static void clear() {
        STATE.remove();
    }

    /**
     * 把一轮请求里的消息列表转成可读预览。
     * 关键看点：第 2 轮开始的请求里会出现 TOOL_RESULT 消息——
     * 这就是"工具结果在下一轮请求中回传给模型"的直接证据。
     */
    static List<String> buildRequestPreviews(List<ChatMessage> messages) {
        List<String> previews = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage systemMessage) {
                previews.add("SYSTEM: " + truncate(systemMessage.text()));
            } else if (message instanceof UserMessage userMessage) {
                previews.add("USER: " + truncate(userMessage.singleText()));
            } else if (message instanceof AiMessage aiMessage) {
                if (aiMessage.hasToolExecutionRequests()) {
                    List<String> calls = aiMessage.toolExecutionRequests().stream()
                            .map(r -> r.name() + "(" + truncate(r.arguments(), 100) + ")")
                            .toList();
                    previews.add("AI→tool_calls: " + String.join(" + ", calls));
                } else {
                    previews.add("AI: " + truncate(aiMessage.text()));
                }
            } else if (message instanceof ToolExecutionResultMessage resultMessage) {
                previews.add("TOOL_RESULT[" + resultMessage.toolName() + "]: " + truncate(resultMessage.text()));
            } else {
                previews.add(message.type() + ": (未展开)");
            }
        }
        return previews;
    }

    private static String truncate(String text) {
        return truncate(text, PREVIEW_MAX_LENGTH);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }

    private static Integer sum(Integer a, Integer b) {
        return (a == null ? 0 : a) + b;
    }

    private static class TraceState {
        final String traceId;
        final String question;
        final LocalDateTime startedAt = LocalDateTime.now();
        final List<TraceRound> rounds = new ArrayList<>();
        Integer totalInputTokens;
        Integer totalOutputTokens;
        List<String> pendingRequestMessages;
        long pendingStartNanos;

        TraceState(String traceId, String question) {
            this.traceId = traceId;
            this.question = question;
        }
    }
}
