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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeOptimizeSkillImplTest {

    @Mock
    private AIService aiService;

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @InjectMocks
    private CodeOptimizeSkillImpl skill;

    @Test
    @DisplayName("name() 返回 code_optimize")
    void name_ReturnsCodeOptimize() {
        assertEquals("code_optimize", skill.name());
    }

    @Test
    @DisplayName("性能优化类型")
    void execute_PerformanceOptimize() {
        when(aiService.optimize(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertTrue(req.getQuestion().contains("性能优化"));
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer("优化建议");
            return resp;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "for(int i=0;i<n;i++){}");
        params.put("optimizeType", "performance");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("performance", data.get("optimizeType"));
    }

    @Test
    @DisplayName("可读性优化类型")
    void execute_ReadabilityOptimize() {
        when(aiService.optimize(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertTrue(req.getQuestion().contains("可读性优化"));
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer("可读性优化建议");
            return resp;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "test");
        params.put("optimizeType", "readability");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("Bug 修复优化类型")
    void execute_BugfixOptimize() {
        when(aiService.optimize(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertTrue(req.getQuestion().contains("Bug修复"));
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer("Bug 已修复");
            return resp;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "var x = null; x.toString();");
        params.put("optimizeType", "bugfix");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("默认综合优化类型")
    void execute_DefaultOptimizeTypeAll() {
        when(aiService.optimize(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertTrue(req.getQuestion().contains("综合优化"));
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer("综合优化建议");
            return resp;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "test");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("code 从上下文获取")
    void execute_CodeFromContext() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("优化完成");
        when(aiService.optimize(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_code", "public class FromContext {}");

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("code 从数据库回源获取")
    void execute_CodeFromDatabase() {
        CodeSnippet snippet = new CodeSnippet();
        snippet.setId(5L);
        snippet.setCodeContent("public class FromDB {}");
        when(codeSnippetRepository.findById(5L)).thenReturn(Optional.of(snippet));

        AIChatResponse response = new AIChatResponse();
        response.setAnswer("优化完成");
        when(aiService.optimize(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_id", 5L);

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("languageType 从上下文获取，默认 Java")
    void execute_LanguageFromContext() {
        when(aiService.optimize(any(AIChatRequest.class))).thenAnswer(invocation -> {
            AIChatRequest req = invocation.getArgument(0);
            assertEquals("TypeScript", req.getLanguageType());
            AIChatResponse resp = new AIChatResponse();
            resp.setAnswer("ok");
            return resp;
        });

        Map<String, Object> params = new HashMap<>();
        params.put("code", "const x = 1;");
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_language", "TypeScript");

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("写入上下文 optimize_answer 和 optimize_suggestions")
    void execute_WritesContext() {
        AIChatResponse response = new AIChatResponse();
        response.setAnswer("优化方案");
        response.setSuggestions(java.util.List.of("移除未使用的变量", "使用 Stream API"));
        when(aiService.optimize(any(AIChatRequest.class))).thenReturn(response);

        Map<String, Object> params = new HashMap<>();
        params.put("code", "code");
        Map<String, Object> context = new HashMap<>();

        skill.execute(params, context);

        assertEquals("优化方案", context.get("optimize_answer"));
        assertNotNull(context.get("optimize_suggestions"));
    }

    @Test
    @DisplayName("异常时捕获并返回失败")
    void execute_Exception_ReturnsFailure() {
        when(aiService.optimize(any())).thenThrow(new RuntimeException("AI 优化服务异常"));

        Map<String, Object> params = Map.of("code", "test");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("AI 优化服务异常"));
    }
}
