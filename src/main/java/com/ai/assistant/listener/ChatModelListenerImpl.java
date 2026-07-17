package com.ai.assistant.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM 模型监听器
 * 监听每一次 LLM 请求、响应和错误
 * 用于日志记录、指标采集、链路追踪
 */
public class ChatModelListenerImpl implements ChatModelListener {

    private static final Logger log = LoggerFactory.getLogger(ChatModelListenerImpl.class);

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("[LLM Request] {}", requestContext.chatRequest());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("[LLM Response] {}", responseContext.chatResponse());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.error("[LLM Error] {}", errorContext.error().getMessage(), errorContext.error());
    }
}
