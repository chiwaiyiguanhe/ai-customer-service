package com.ai.assistant.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 同步对话 AI 服务
 * LangChain4j 的 AiService 高级抽象，自动处理：
 * - ChatMemory 会话记忆管理
 * - Function Calling 工具调用
 * - SystemMessage 系统提示词
 */
@AiService
public interface Assistant {

    @SystemMessage("你是一个电商平台的智能客服助手，可以帮用户查询订单、统计消费、推荐商品。" +
                   "请用中文回答，语气友好专业。")
    String chat(String userMessage);
}
