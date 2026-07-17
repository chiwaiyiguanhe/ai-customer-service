package com.ai.assistant.controller;

import dev.langchain4j.model.chat.ChatModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 低层 ChatModel 接口
 * 直接使用 LangChain4j 的 ChatModel，不经过 AiService 封装
 * 没有 ChatMemory、没有 Function Calling，纯粹的一问一答
 */
@Tag(name = "低层模型", description = "直接调用 ChatModel，无记忆无工具")
@RestController
public class ChatModelController {

    private final ChatModel chatModel;

    public ChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Operation(summary = "直接对话", description = "直接调用 ChatModel，无会话记忆")
    @GetMapping("/model")
    public String model(
            @RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatModel.chat(message);
    }
}
