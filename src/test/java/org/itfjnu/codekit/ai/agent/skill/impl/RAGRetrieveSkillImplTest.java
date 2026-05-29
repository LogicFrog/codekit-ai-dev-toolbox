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
class RAGRetrieveSkillImplTest {

    @Mock
    private SearchService searchService;

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @InjectMocks
    private RAGRetrieveSkillImpl skill;

    @Test
    @DisplayName("name() 返回 rag_retrieve")
    void name_ReturnsRagRetrieve() {
        assertEquals("rag_retrieve", skill.name());
    }

    @Test
    @DisplayName("query 为空返回失败")
    void execute_EmptyQuery_ReturnsFailure() {
        Map<String, Object> params = Map.of("query", "");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("为空"));
    }

    @Test
    @DisplayName("语义搜索返回结果成功")
    void execute_ValidQuery_ReturnsResults() {
        SearchResponse item = new SearchResponse();
        item.setId(10L);
        item.setFileName("RedisConfig.java");
        item.setCodePreview("public class RedisConfig {}");
        item.setLanguageType("Java");
        item.setRelevanceScore(0.92);

        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(10L)).thenReturn(Optional.of(createSnippet(10L, "full code here")));

        Map<String, Object> params = Map.of("query", "Redis 连接池配置");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(1, data.get("total"));
        assertEquals(0.92, data.get("topScore"));
    }

    @Test
    @DisplayName("minScore 过滤低相关性结果")
    void execute_MinScoreFilter_FiltersResults() {
        SearchResponse lowScore = new SearchResponse();
        lowScore.setId(11L);
        lowScore.setFileName("Low.java");
        lowScore.setRelevanceScore(0.5);

        SearchResponse highScore = new SearchResponse();
        highScore.setId(12L);
        highScore.setFileName("High.java");
        highScore.setRelevanceScore(0.85);

        Page<SearchResponse> page = new PageImpl<>(List.of(lowScore, highScore));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(12L)).thenReturn(Optional.of(createSnippet(12L, "high score code")));

        Map<String, Object> params = Map.of("query", "test", "minScore", 0.7);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(1, data.get("total"));
    }

    @Test
    @DisplayName("无结果时返回失败")
    void execute_NoResults_ReturnsFailure() {
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(new PageImpl<>(List.of()));

        Map<String, Object> params = Map.of("query", "不存在的内容");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("无结果"));
    }

    @Test
    @DisplayName("topK 参数限制返回数量")
    void execute_TopKLimit() {
        SearchResponse item = new SearchResponse();
        item.setId(20L);
        item.setRelevanceScore(0.9);
        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(20L)).thenReturn(Optional.of(createSnippet(20L, "code")));

        Map<String, Object> params = new HashMap<>();
        params.put("query", "test");
        params.put("topK", 3);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(3, data.get("topK"));
    }

    @Test
    @DisplayName("写入上下文供后续 Skill 使用")
    void execute_WritesContext() {
        SearchResponse item = new SearchResponse();
        item.setId(30L);
        item.setFileName("Target.java");
        item.setCodePreview("target preview");
        item.setLanguageType("Python");
        item.setRelevanceScore(0.88);

        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(30L)).thenReturn(Optional.of(createSnippet(30L, "full target code")));

        Map<String, Object> params = Map.of("query", "目标代码");
        Map<String, Object> context = new HashMap<>();

        skill.execute(params, context);

        assertEquals("目标代码", context.get("retrieve_query"));
        assertEquals(30L, context.get("search_top_id"));
        assertEquals("Python", context.get("search_top_language"));
        assertEquals("full target code", context.get("search_top_code"));
    }

    @Test
    @DisplayName("includeCode=false 时不注入完整代码")
    void execute_IncludeCodeFalse_NoFullCode() {
        SearchResponse item = new SearchResponse();
        item.setId(40L);
        item.setRelevanceScore(0.9);
        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);

        Map<String, Object> params = new HashMap<>();
        params.put("query", "test");
        params.put("includeCode", false);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("异常时捕获并返回失败")
    void execute_Exception_ReturnsFailure() {
        when(searchService.semanticSearch(any())).thenThrow(new RuntimeException("向量服务异常"));

        Map<String, Object> params = Map.of("query", "test");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("向量服务异常"));
    }

    @Test
    @DisplayName("default minScore=0.0 不过滤结果")
    void execute_DefaultMinScore_NoFilter() throws Exception {
        SearchResponse item = new SearchResponse();
        item.setId(50L);
        item.setRelevanceScore(0.1);
        Page<SearchResponse> page = new PageImpl<>(List.of(item));
        when(searchService.semanticSearch(any(SearchRequest.class))).thenReturn(page);
        when(codeSnippetRepository.findById(50L)).thenReturn(Optional.of(createSnippet(50L, "code")));

        Map<String, Object> params = Map.of("query", "test");

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
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
