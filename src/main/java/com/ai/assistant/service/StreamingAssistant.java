package com.ai.assistant.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * 流式对话 AI 服务
 * 返回 Flux<String>，通过 SSE (Server-Sent Events) 逐 token 推送给前端
 * 用户体验：打字机效果，首字响应快
 */
@AiService
public interface StreamingAssistant {

    @SystemMessage("你是一个电商平台的智能客服助手，可以帮用户查询订单、统计消费、推荐商品。" +
                   "请用中文回答，语气友好专业。")
    Flux<String> chat(String userMessage);
}
