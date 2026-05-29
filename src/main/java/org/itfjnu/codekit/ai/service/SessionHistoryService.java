package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.dto.ChatMessage;

import java.util.List;

public interface SessionHistoryService {

    Boolean appendUserMessage(String sessionId, String content);

    Boolean appendAssistantMessage(String sessionId, String content);

    List<ChatMessage> getRecentMessages(String sessionId, int maxRounds);

    /**
     * 按 Token 预算获取最近的上下文消息
     * 从最新消息向前累加，直到达到 token 预算上限
     *
     * @param sessionId 会话 ID
     * @param maxTokens Token 预算上限
     * @return 在预算范围内的最近消息列表（保持时间顺序）
     */
    List<ChatMessage> getRecentMessagesByTokenBudget(String sessionId, int maxTokens);

    /**
     * 获取会话总 Token 用量
     */
    int getSessionTokenUsage(String sessionId);

    Boolean clearSession(String sessionId);
}
