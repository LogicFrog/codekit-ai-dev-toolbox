package org.itfjnu.codekit.ai.controller;

import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.ai.service.SessionHistoryService;
import org.itfjnu.codekit.ai.model.ChatMessage;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIControllerTest {

    @Test
    @DisplayName("chat 接口：应透传请求并返回成功包装")
    void testChat_ShouldDelegateAndWrapSuccess() {
        StubAIService stubAIService = new StubAIService();
        StubSessionHistoryService stubSessionHistoryService = new StubSessionHistoryService();
        AIController controller = new AIController(stubAIService, stubSessionHistoryService);

        AIChatRequest request = new AIChatRequest();
        request.setQuestion("解释一下这个类");
        request.setSessionId("session-chat-1");

        ApiResponse<AIChatResponse> response = controller.chat(request);

        assertNotNull(response);
        assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
        assertNotNull(response.getData());
        assertEquals("stub-chat-answer", response.getData().getAnswer());
        assertEquals("session-chat-1", stubAIService.lastChatRequest.getSessionId());
        assertEquals("解释一下这个类", stubAIService.lastChatRequest.getQuestion());
    }

    @Test
    @DisplayName("explain 接口：应透传代码请求并返回建议")
    void testExplain_ShouldDelegateAndWrapSuccess() {
        StubAIService stubAIService = new StubAIService();
        StubSessionHistoryService stubSessionHistoryService = new StubSessionHistoryService();
        AIController controller = new AIController(stubAIService, stubSessionHistoryService);

        AIChatRequest request = new AIChatRequest();
        request.setCode("public class A {}");
        request.setLanguageType("Java");
        request.setSessionId("session-exp-1");

        ApiResponse<AIChatResponse> response = controller.explain(request);

        assertNotNull(response);
        assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
        assertNotNull(response.getData());
        assertEquals("stub-explain-answer", response.getData().getAnswer());
        assertTrue(response.getData().getSuggestions().contains("拆分方法"));
        assertEquals("Java", stubAIService.lastExplainRequest.getLanguageType());
        assertEquals("session-exp-1", stubAIService.lastExplainRequest.getSessionId());
    }

    @Test
    @DisplayName("clearSession 接口：应调用会话服务并返回布尔值")
    void testClearSession_ShouldDelegateAndWrapSuccess() {
        StubAIService stubAIService = new StubAIService();
        StubSessionHistoryService stubSessionHistoryService = new StubSessionHistoryService();
        AIController controller = new AIController(stubAIService, stubSessionHistoryService);

        ApiResponse<Boolean> response = controller.clearSession("session-1");

        assertNotNull(response);
        assertEquals(ErrorCode.SUCCESS.getCode(), response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
        assertEquals("session-1", stubSessionHistoryService.lastClearedSessionId);
    }

    private static final class StubAIService implements AIService {
        private AIChatRequest lastChatRequest;
        private AIChatRequest lastExplainRequest;

        @Override
        public AIChatResponse chat(AIChatRequest request) {
            this.lastChatRequest = request;
            return AIChatResponse.builder()
                    .answer("stub-chat-answer")
                    .build();
        }

        @Override
        public SseEmitter chatStream(AIChatRequest request) {
            this.lastChatRequest = request;
            return new SseEmitter(1000L);
        }

        @Override
        public AIChatResponse explain(AIChatRequest request) {
            this.lastExplainRequest = request;
            return AIChatResponse.builder()
                    .answer("stub-explain-answer")
                    .suggestions(List.of("拆分方法", "补充异常处理"))
                    .build();
        }

        @Override
        public String getProviderName() {
            return "stub";
        }
    }

    private static final class StubSessionHistoryService implements SessionHistoryService {
        private String lastClearedSessionId;

        @Override
        public Boolean appendUserMessage(String sessionId, String content) {
            return Boolean.TRUE;
        }

        @Override
        public Boolean appendAssistantMessage(String sessionId, String content) {
            return Boolean.TRUE;
        }

        @Override
        public List<ChatMessage> getRecentMessages(String sessionId, int maxRounds) {
            return List.of();
        }

        @Override
        public Boolean clearSession(String sessionId) {
            this.lastClearedSessionId = sessionId;
            return Boolean.TRUE;
        }
    }
}
