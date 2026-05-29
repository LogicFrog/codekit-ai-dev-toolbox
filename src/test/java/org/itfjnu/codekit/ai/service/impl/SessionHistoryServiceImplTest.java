package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionHistoryServiceImplTest {

    @TempDir
    Path tempDir;

    private SessionHistoryServiceImpl service;
    private AIProperties aiProperties;

    @BeforeEach
    void setUp() {
        aiProperties = new AIProperties();
        aiProperties.setMaxContextTokens(4096);
        aiProperties.setMaxContextRounds(4);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        service = new SessionHistoryServiceImpl(aiProperties, objectMapper);
        setField(service, "storeFile", tempDir.resolve("ai-sessions.json").toString());
        service.init();
    }

    @Test
    @DisplayName("添加用户消息后能通过 getRecentMessages 获取")
    void appendUserMessage_CanRetrieve() {
        service.appendUserMessage("session-1", "你好，请帮我解释这段代码");

        List<ChatMessage> messages = service.getRecentMessages("session-1", 4);
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("你好，请帮我解释这段代码", messages.get(0).getContent());
    }

    @Test
    @DisplayName("添加助手消息")
    void appendAssistantMessage_Works() {
        service.appendAssistantMessage("session-1", "这是一段 Java 代码...");

        List<ChatMessage> messages = service.getRecentMessages("session-1", 4);
        assertEquals(1, messages.size());
        assertEquals("assistant", messages.get(0).getRole());
    }

    @Test
    @DisplayName("getRecentMessages 限制返回轮数")
    void getRecentMessages_LimitedByRounds() {
        for (int i = 0; i < 6; i++) {
            service.appendUserMessage("session-1", "question " + i);
            service.appendAssistantMessage("session-1", "answer " + i);
        }

        List<ChatMessage> messages = service.getRecentMessages("session-1", 2);

        assertEquals(4, messages.size());
    }

    @Test
    @DisplayName("getRecentMessages 不存在的 session 返回空列表")
    void getRecentMessages_NonExistingSession_ReturnsEmpty() {
        List<ChatMessage> messages = service.getRecentMessages("nonexistent", 4);

        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("超过 20 条消息时自动修剪最早的消息")
    void append_ExceedsMaxMessages_TrimsOldest() {
        for (int i = 0; i < 22; i++) {
            service.appendUserMessage("session-1", "message " + i);
        }

        List<ChatMessage> messages = service.getRecentMessages("session-1", 20);

        assertTrue(messages.size() <= 20);
        assertFalse(messages.get(0).getContent().contains("message 0"));
    }

    @Test
    @DisplayName("消息内容超过 8000 字符时截断")
    void append_ExceedsMaxChars_Truncated() {
        String longContent = "A".repeat(10000);
        service.appendUserMessage("session-1", longContent);

        List<ChatMessage> messages = service.getRecentMessages("session-1", 4);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getContent().length() <= 8000);
    }

    @Test
    @DisplayName("null 内容被转为空字符串")
    void append_NullContent_StoredAsEmpty() {
        service.appendUserMessage("session-1", null);

        List<ChatMessage> messages = service.getRecentMessages("session-1", 4);
        assertEquals(1, messages.size());
        assertEquals("", messages.get(0).getContent());
    }

    @Test
    @DisplayName("clearSession 清除指定 session")
    void clearSession_RemovesSession() {
        service.appendUserMessage("session-1", "test message");
        assertFalse(service.getRecentMessages("session-1", 4).isEmpty());

        Boolean result = service.clearSession("session-1");

        assertTrue(result);
        assertTrue(service.getRecentMessages("session-1", 4).isEmpty());
    }

    @Test
    @DisplayName("clearSession 不存在的 session 返回 false")
    void clearSession_NonExistingSession_ReturnsFalse() {
        Boolean result = service.clearSession("nonexistent");

        assertFalse(result);
    }

    @Test
    @DisplayName("getRecentMessagesByTokenBudget 不存在的 session 返回空")
    void getRecentMessagesByTokenBudget_NonExistingSession_ReturnsEmpty() {
        List<ChatMessage> messages = service.getRecentMessagesByTokenBudget("nonexistent", 4096);

        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("getSessionTokenUsage 不存在的 session 返回 0")
    void getSessionTokenUsage_NonExistingSession_ReturnsZero() {
        int usage = service.getSessionTokenUsage("nonexistent");

        assertEquals(0, usage);
    }

    @Test
    @DisplayName("getSessionTokenUsage 存在消息时返回大于 0")
    void getSessionTokenUsage_ExistingSession_ReturnsNonZero() {
        service.appendUserMessage("session-1", "一段含有 token 的消息内容用于测试");

        int usage = service.getSessionTokenUsage("session-1");

        assertTrue(usage > 0);
    }

    @Test
    @DisplayName("多 session 隔离")
    void multipleSessions_Isolated() {
        service.appendUserMessage("session-A", "message A");
        service.appendUserMessage("session-B", "message B");

        List<ChatMessage> messagesA = service.getRecentMessages("session-A", 4);
        List<ChatMessage> messagesB = service.getRecentMessages("session-B", 4);

        assertEquals(1, messagesA.size());
        assertEquals(1, messagesB.size());
        assertEquals("message A", messagesA.get(0).getContent());
        assertEquals("message B", messagesB.get(0).getContent());
    }

    @Test
    @DisplayName("appendUserMessage 始终返回 TRUE")
    void appendUserMessage_ReturnsTrue() {
        Boolean result = service.appendUserMessage("session-1", "test");

        assertTrue(result);
    }

    @Test
    @DisplayName("appendAssistantMessage 始终返回 TRUE")
    void appendAssistantMessage_ReturnsTrue() {
        Boolean result = service.appendAssistantMessage("session-1", "test");

        assertTrue(result);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
