package com.ai.assistant.controller;

import com.ai.assistant.common.Result;
import com.ai.assistant.dto.ChatRequest;
import com.ai.assistant.service.Assistant;
import com.ai.assistant.service.StreamingAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * AI 对话接口
 * 提供同步对话和流式对话两种方式
 */
@Tag(name = "AI 对话", description = "智能客服对话接口")
@RestController
@RequestMapping("/api/chat")
public class AssistantController {

    private final Assistant assistant;
    private final StreamingAssistant streamingAssistant;

    public AssistantController(Assistant assistant, StreamingAssistant streamingAssistant) {
        this.assistant = assistant;
        this.streamingAssistant = streamingAssistant;
    }

    @Operation(summary = "同步对话（GET）", description = "通过 URL 参数发送消息，等待 LLM 完整生成后返回")
    @GetMapping
    public Result<String> chat(@RequestParam(value = "message", defaultValue = "你好") String message) {
        return Result.success(assistant.chat(message));
    }

    @Operation(summary = "同步对话（POST）", description = "通过 JSON Body 发送消息，带参数校验")
    @PostMapping
    public Result<String> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(assistant.chat(request.getMessage()));
    }

    @Operation(summary = "流式对话", description = "通过 SSE 逐 token 推送 LLM 响应")
    @GetMapping(value = "/stream", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam(value = "message", defaultValue = "你好") String message) {
        return streamingAssistant.chat(message);
    }
}
