package org.itfjnu.codekit.ai.agent.skill.impl;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIExplainSkillImplTest {

    @Mock
    private AIService aiService;

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @InjectMocks
    private AIExplainSkillImpl skill;

    @Test
    @DisplayName("name() 返回 ai_explain")
    void name_ReturnsAiExplain() {
        assertEquals("ai_explain", skill.name());
    }

    @Test
    @DisplayName("params 有 code 时直接使用")
    void execute_CodeInParams_UsesCode() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("这段代码是一个工厂模式实现");
        response.setSuggestions(List.of("建议使用依赖注入"));

        when(aiService.explain(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        params.put("code", "public class Factory {}");
        params.put("languageType", "Java");
        params.put("question", "解释这段代码");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("这段代码是一个工厂模式实现", data.get("answer"));
        assertEquals(List.of("建议使用依赖注入"), data.get("suggestions"));
    }

    @Test
    @DisplayName("code 为空时从上下文 search_top_code 获取")
    void execute_CodeFromContext_UsesContextCode() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("代码分析结果");
        response.setSuggestions(List.of());
        when(aiService.explain(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_code", "public class FromContext {}");
        context.put("search_top_language", "Java");

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("code 为空时从上下文 search_top_preview 回退")
    void execute_CodeFromPreview_FallsBackToPreview() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("分析结果");
        when(aiService.explain(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_preview", "public class Preview {}");

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("code 为空且有 search_top_id 时从数据库回源获取")
    void execute_CodeFromDatabase_FetchesById() {
        CodeSnippet snippet = new CodeSnippet();
        snippet.setId(1L);
        snippet.setCodeContent("public class FromDatabase {}");

        when(codeSnippetRepository.findById(1L)).thenReturn(Optional.of(snippet));

        AIChatResponse response = new AIChatResponse();
        response.setAnswer("数据库代码分析");
        when(aiService.explain(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_id", 1L);

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("languageType 为空时从上下文获取，默认 Java")
    void execute_LanguageFromContextOrDefault() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("ok");

        when(aiService.explain(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertEquals("Python", req.getLanguageType());
            return response;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "def hello(): pass");
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_language", "Python");

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("默认 question 值")
    void execute_DefaultQuestion() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("默认分析");
        when(aiService.explain(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertTrue(req.getQuestion().contains("解释"));
            return response;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "public class Test {}");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("写入上下文 explain_answer 和 explain_suggestions")
    void execute_WritesContext() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("分析完成");
        response.setSuggestions(List.of("建议1", "建议2"));
        when(aiService.explain(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        params.put("code", "code content");
        Map<String, Object> context = new HashMap<>();

        skill.execute(params, context);

        assertEquals("分析完成", context.get("explain_answer"));
        assertEquals(List.of("建议1", "建议2"), context.get("explain_suggestions"));
    }

    @Test
    @DisplayName("异常时捕获并返回失败")
    void execute_Exception_ReturnsFailure() {
        when(aiService.explain(any())).thenThrow(new RuntimeException("AI 服务故障"));

        Map<String, Object> params = Map.of("code", "test");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("AI 服务故障"));
    }

    @Test
    @DisplayName("response.suggestions 为 null 时使用空列表")
    void execute_NullSuggestions_UsesEmptyList() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("分析");
        response.setSuggestions(null);
        when(aiService.explain(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = Map.of("code", "test");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue(((List<?>) data.get("suggestions")).isEmpty());
    }
}
