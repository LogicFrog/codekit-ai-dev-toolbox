package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.model.ChatMessage;

import java.util.List;

public interface SessionHistoryService {

    Boolean appendUserMessage(String sessionId, String content);

    Boolean appendAssistantMessage(String sessionId, String content);

    List<ChatMessage> getRecentMessages(String sessionId, int maxRounds);

    Boolean clearSession(String sessionId);
}
