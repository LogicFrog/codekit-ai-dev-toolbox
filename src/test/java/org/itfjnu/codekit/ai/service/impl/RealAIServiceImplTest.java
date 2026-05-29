package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.ChatMessage;
import org.itfjnu.codekit.ai.prompt.service.impl.PromptTemplateServiceImpl;
import org.itfjnu.codekit.ai.service.SessionHistoryService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.ResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RealAIServiceImplTest {

    private RealAIServiceImpl realAIService;

    @BeforeEach
    void setUp() {
        AIProperties aiProperties = new AIProperties();
        aiProperties.setApiKey("");
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        PromptTemplateServiceImpl promptTemplateService = new PromptTemplateServiceImpl(resolver);
        promptTemplateService.init();
        realAIService = new RealAIServiceImpl(aiProperties, new ObjectMapper(), new NoopSessionHistoryService(), promptTemplateService);
    }

    @Test
    @DisplayName("测试 1：API Key 未配置时，chat() 抛出配置异常")
    void testChat_ApiKeyNotConfigured() {
        AIChatRequest request = new AIChatRequest();
        request.setQuestion("什么是 Spring Boot？");

        ServiceException ex = assertThrows(ServiceException.class, () -> realAIService.chat(request));
        assertEquals(ErrorCode.CONFIG_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("测试 2：getProviderName() 返回有效的提供商名称")
    void testGetProviderName() {
        String providerName = realAIService.getProviderName();
        assertEquals("豆包 (Doubao)", providerName, "默认 provider 应返回豆包");
    }

    @Test
    @DisplayName("测试 3：API Key 未配置时，explain() 抛出配置异常")
    void testExplain_ApiKeyNotConfigured() {
        AIChatRequest request = new AIChatRequest();
        request.setCode("public class Hello {}");
        request.setLanguageType("Java");

        ServiceException ex = assertThrows(ServiceException.class, () -> realAIService.explain(request));
        assertEquals(ErrorCode.CONFIG_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("测试 4：chat() 空问题时仍优先抛出配置异常")
    void testChat_EmptyQuestion() {
        AIChatRequest request = new AIChatRequest();
        request.setQuestion("");
        ServiceException ex = assertThrows(ServiceException.class, () -> realAIService.chat(request));
        assertEquals(ErrorCode.CONFIG_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("测试 5：chat() null 问题时仍优先抛出配置异常")
    void testChat_NullQuestion() {
        AIChatRequest request = new AIChatRequest();
        request.setQuestion(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> realAIService.chat(request));
        assertEquals(ErrorCode.CONFIG_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("测试 6：explain() null 代码时仍优先抛出配置异常")
    void testExplain_NullCode() {
        AIChatRequest request = new AIChatRequest();
        request.setCode(null);
        request.setLanguageType("Java");
        ServiceException ex = assertThrows(ServiceException.class, () -> realAIService.explain(request));
        assertEquals(ErrorCode.CONFIG_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("测试 7：chat() 携带 sessionId 时请求对象字段保持不变")
    void testChat_WithSessionId_RequestKeepsValue() {
        AIChatRequest request = new AIChatRequest();
        request.setQuestion("继续上一个问题");
        request.setSessionId("session-001");

        assertThrows(ServiceException.class, () -> realAIService.chat(request));
        assertEquals("session-001", request.getSessionId());
    }

    private static final class NoopSessionHistoryService implements SessionHistoryService {
        @Override
        public Boolean appendUserMessage(String sessionId, String content) {
            return Boolean.TRUE;
        }

        @Override
        public Boolean appendAssistantMessage(String sessionId, String content) {
            return Boolean.TRUE;
        }

        @Override
        public java.util.List<ChatMessage> getRecentMessages(String sessionId, int maxRounds) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<ChatMessage> getRecentMessagesByTokenBudget(String sessionId, int maxTokens) {
            return java.util.List.of();
        }

        @Override
        public int getSessionTokenUsage(String sessionId) {
            return 0;
        }

        @Override
        public Boolean clearSession(String sessionId) {
            return Boolean.TRUE;
        }
    }
}
