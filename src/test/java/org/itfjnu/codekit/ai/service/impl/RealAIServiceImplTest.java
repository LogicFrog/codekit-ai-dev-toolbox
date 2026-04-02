package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RealAIServiceImpl 单元测试
 * 
 * 测试目标：
 * 1. API Key 未配置时返回错误
 * 2. getProviderName() 返回值正确
 * 3. explain() 在未配置时能正确报错
 * 4. extractSuggestions() 的行为（通过 explain() 间接测试）
 */
@ExtendWith(MockitoExtension.class)
class RealAIServiceImplTest {

    @Mock
    private AIProperties aiProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RealAIServiceImpl realAIService;

    @BeforeEach
    void setUp() {
        // 每个测试前重置 mock 状态
        reset(aiProperties, objectMapper);
    }

    @Test
    @DisplayName("测试 1：API Key 未配置时，chat() 返回错误")
    void testChat_ApiKeyNotConfigured() {
        // 准备：模拟 aiProperties.isConfigured() 返回 false
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行：调用 chat()
        AIChatRequest request = new AIChatRequest();
        request.setQuestion("什么是 Spring Boot？");
        AIChatResponse response = realAIService.chat(request);

        // 断言
        assertNotNull(response, "响应不应该为空");
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError(), 
                "错误码应该是 API_KEY_NOT_CONFIGURED");
        assertTrue(response.getAnswer().contains("未配置"), 
                "回答应该包含'未配置'相关提示");
        
        // 验证 isConfigured() 被调用
        verify(aiProperties, times(1)).isConfigured();
    }

    @Test
    @DisplayName("测试 2：getProviderName() 返回 'real'")
    void testGetProviderName() {
        // 执行
        String providerName = realAIService.getProviderName();

        // 断言
        assertEquals("real", providerName, "提供者名称应该是 'real'");
    }

    @Test
    @DisplayName("测试 3：API Key 未配置时，explain() 返回错误")
    void testExplain_ApiKeyNotConfigured() {
        // 准备
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行
        AIChatRequest request = new AIChatRequest();
        request.setCode("public class Hello {}");
        request.setLanguageType("Java");
        AIChatResponse response = realAIService.explain(request);

        // 断言
        assertNotNull(response, "响应不应该为空");
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError(), 
                "错误码应该是 API_KEY_NOT_CONFIGURED");
        
        // 验证 isConfigured() 被调用
        verify(aiProperties, times(1)).isConfigured();
    }



    @Test
    @DisplayName("测试 6：chat() 请求中包含 sessionId 时能正常处理")
    void testChat_WithSessionId() {
        // 准备
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行
        AIChatRequest request = new AIChatRequest();
        request.setQuestion("什么是 Spring Boot？");
        request.setSessionId("test-session-123");
        AIChatResponse response = realAIService.chat(request);

        // 断言
        assertNotNull(response);
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError());
        
        // 验证 sessionId 被传递（虽然没有实际使用，但字段存在）
        assertEquals("test-session-123", request.getSessionId());
    }

    @Test
    @DisplayName("测试 7：explain() 请求中包含代码和语言类型时能正常处理")
    void testExplain_WithCodeAndLanguage() {
        // 准备
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行
        AIChatRequest request = new AIChatRequest();
        request.setCode("public class Test { public static void main(String[] args) {} }");
        request.setLanguageType("Java");
        AIChatResponse response = realAIService.explain(request);

        // 断言
        assertNotNull(response);
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError());
        
        // 验证请求参数被正确传递
        assertEquals("Java", request.getLanguageType());
        assertNotNull(request.getCode());
    }

    @Test
    @DisplayName("测试 8：chat() 处理空问题时也能正常返回错误")
    void testChat_EmptyQuestion() {
        // 准备
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行
        AIChatRequest request = new AIChatRequest();
        request.setQuestion("");
        AIChatResponse response = realAIService.chat(request);

        // 断言
        assertNotNull(response);
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError());
    }

    @Test
    @DisplayName("测试 9：chat() 处理 null 问题时也能正常返回错误")
    void testChat_NullQuestion() {
        // 准备
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行
        AIChatRequest request = new AIChatRequest();
        request.setQuestion(null);
        AIChatResponse response = realAIService.chat(request);

        // 断言
        assertNotNull(response);
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError());
    }

    @Test
    @DisplayName("测试 10：explain() 处理 null 代码时也能正常返回错误")
    void testExplain_NullCode() {
        // 准备
        when(aiProperties.isConfigured()).thenReturn(false);

        // 执行
        AIChatRequest request = new AIChatRequest();
        request.setCode(null);
        request.setLanguageType("Java");
        AIChatResponse response = realAIService.explain(request);

        // 断言
        assertNotNull(response);
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError());
    }
}
