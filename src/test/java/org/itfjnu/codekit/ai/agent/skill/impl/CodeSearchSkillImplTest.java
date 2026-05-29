package org.itfjnu.codekit.ai.agent.skill.impl;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeSearchSkillImplTest {

    @Mock
    private SearchService searchService;

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @InjectMocks
    private CodeSearchSkillImpl skill;

    @Test
    @DisplayName("name() 返回 code_search")
    void name_ReturnsCodeSearch() {
        assertEquals("code_search", skill.name());
    }

    @Test
    @DisplayName("关键词为空且无 fallback 时返回失败")
    void execute_EmptyKeyword_ReturnsFailure() {
        Map<String, Object> params = Map.of("keyword", "");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("为空"));
    }

    @Test
    @DisplayName("语义搜索返回结果成功")
    void execute_SemanticSearch_ReturnsResults() {
        SearchResponse item = new SearchResponse();
        item.setId(1L);
        item.setFileName("Test.java");
        item.setCodePreview("public class Test {}");
        item.setLanguageType("Java");
        item.setRelevanceScore(0.95);

        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(1L)).thenReturn(Optional.of(createSnippet(1L, "public class Test {}")));

        Map<String, Object> params = Map.of("keyword", "redis", "mode", "semantic");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(1L, data.get("total"));
        assertEquals("semantic", data.get("mode"));
    }

    @Test
    @DisplayName("关键词搜索返回结果成功")
    void execute_KeywordSearch_ReturnsResults() {
        SearchResponse item = new SearchResponse();
        item.setId(2L);
        item.setFileName("Config.java");
        item.setCodePreview("public class Config {}");
        item.setLanguageType("Java");

        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.keywordSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(2L)).thenReturn(Optional.of(createSnippet(2L, "public class Config {}")));

        Map<String, Object> params = Map.of("keyword", "config", "mode", "keyword");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(1L, data.get("total"));
        assertEquals("keyword", data.get("mode"));
    }

    @Test
    @DisplayName("优先语义搜索，空结果时降级关键词搜索")
    void execute_SemanticEmpty_FallsBackToKeyword() {
        Page<SearchResponse> emptyPage = new PageImpl<>(List.of());
        SearchResponse item = new SearchResponse();
        item.setId(3L);
        item.setFileName("User.java");
        item.setCodePreview("public class User {}");
        Page<SearchResponse> keywordPage = new PageImpl<>(List.of(item));

        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(emptyPage);
        when(searchService.keywordSearch(any(SearchRequest.class))).thenReturn(keywordPage);
        when(codeSnippetRepository.findById(3L)).thenReturn(Optional.of(createSnippet(3L, "public class User {}")));

        Map<String, Object> params = Map.of("keyword", "user", "mode", "semantic");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("无结果时返回失败")
    void execute_NoResults_ReturnsFailure() {
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(new PageImpl<>(List.of()));
        when(searchService.keywordSearch(any(SearchRequest.class))).thenReturn(new PageImpl<>(List.of()));

        Map<String, Object> params = Map.of("keyword", "nonexistent", "mode", "semantic");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("未找到"));
    }

    @Test
    @DisplayName("写入上下文供后续 Skill 使用")
    void execute_WritesContext() {
        SearchResponse item = new SearchResponse();
        item.setId(5L);
        item.setFileName("Main.java");
        item.setCodePreview("public class Main {}");
        item.setLanguageType("Java");

        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(5L)).thenReturn(Optional.of(createSnippet(5L, "public class Main { void run() {} }")));

        Map<String, Object> params = Map.of("keyword", "main", "mode", "semantic");
        Map<String, Object> context = new HashMap<>();

        skill.execute(params, context);

        assertEquals(1L, context.get("search_total"));
        assertEquals(5L, context.get("search_top_id"));
        assertEquals("Java", context.get("search_top_language"));
        assertEquals("public class Main {}", context.get("search_top_preview"));
        assertEquals("public class Main { void run() {} }", context.get("search_top_code"));
    }

    @Test
    @DisplayName("fallbackKeyword 兜底：关键词为空时使用 fallback 并降级为关键词搜索")
    void execute_EmptyKeywordWithFallback_UsesFallback() {
        SearchResponse item = new SearchResponse();
        item.setId(6L);
        item.setFileName("Fallback.java");
        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.keywordSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(6L)).thenReturn(Optional.of(createSnippet(6L, "fallback code")));

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "");
        params.put("fallbackKeyword", "兜底关键词");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("兜底关键词", data.get("keyword"));
    }

    @Test
    @DisplayName("异常时捕获并返回失败")
    void execute_Exception_ReturnsFailure() {
        when(searchService.semanticSearch(any())).thenThrow(new RuntimeException("服务不可用"));

        Map<String, Object> params = Map.of("keyword", "test", "mode", "semantic");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("服务不可用"));
    }

    private CodeSnippet createSnippet(Long id, String codeContent) {
        CodeSnippet snippet = new CodeSnippet();
        snippet.setId(id);
        snippet.setCodeContent(codeContent);
        snippet.setFileName("test.java");
        snippet.setLanguageType("Java");
        return snippet;
    }
}
