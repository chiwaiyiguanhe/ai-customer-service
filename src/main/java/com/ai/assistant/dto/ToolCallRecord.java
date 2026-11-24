package com.ai.assistant.dto;

/**
 * 一次工具调用记录（模型在某轮请求要求调用的工具）
 */
public record ToolCallRecord(String name, String arguments, String requestId) {
}
