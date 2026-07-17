package com.ai.assistant.config;

import com.ai.assistant.listener.ChatModelListenerImpl;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * AI 服务配置类
 * - 配置 ChatMemory（会话记忆窗口）
 * - 配置 ChatModelListener（LLM 请求/响应监听）
 */
@Configuration
public class AssistantConfiguration {

    /**
     * 使用原型作用域，每个 AiService 实例拥有独立的 ChatMemory
     * 保留最近 10 条消息，超出后自动淘汰最早的消息
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    /**
     * 监听所有 ChatModel 和 StreamingChatModel 的请求/响应/错误
     * 用于日志记录和链路追踪
     */
    @Bean
    ChatModelListener chatModelListener() {
        return new ChatModelListenerImpl();
    }
}
