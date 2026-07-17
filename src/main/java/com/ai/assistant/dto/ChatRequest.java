package com.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求 DTO
 * 用于 POST /api/chat 接口的请求体
 */
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容不能超过500字")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
