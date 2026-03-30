package org.itfjnu.codekit.search.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.common.cache.RedisCacheService;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.model.SearchHistory;
import org.itfjnu.codekit.search.repository.SearchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private CodeSnippetRepository codeSnippetRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private RedisCacheService redisCacheService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SearchServiceImpl searchService;

    private CodeSnippet testSnippet;
    private SearchRequest testRequest;

    @BeforeEach
    void setUp() {
        testSnippet = new CodeSnippet();
        testSnippet.setId(1L);
        testSnippet.setFilePath("/test/Test.java");
        testSnippet.setFileName("Test.java");
        testSnippet.setCodeContent("public class RedisConfig { private String config; }");
        testSnippet.setLanguageType("Java");
        testSnippet.setClassName("RedisConfig");
        testSnippet.setTags(new HashSet<>(Set.of("Redis", "Config")));

        testRequest = new SearchRequest();
        testRequest.setKeyword("Redis");
        testRequest.setPage(0);
        testRequest.setSize(10);
        testRequest.setExactMatch(false);
    }

    @Test
    @DisplayName("关键词搜索 - 正常返回结果")
    void testKeywordSearch_Normal() {
        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.fullTextSearch("Redis"))
                .thenReturn(List.of(testSnippet));
        when(searchHistoryRepository.save(any(SearchHistory.class)))
                .thenReturn(new SearchHistory());

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("RedisConfig", result.getContent().get(0).getClassName());
    }

    @Test
    @DisplayName("搜索页码超界返回空列表")
    void testKeywordSearch_PageOutOfRange() {
        testRequest.setPage(100);
        testRequest.setSize(10);

        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.fullTextSearch("Redis"))
                .thenReturn(List.of(testSnippet));

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Redis缓存命中返回结果")
    void testKeywordSearch_CacheHit() throws Exception {
        String cachedJson = "[{\"id\":1,\"filePath\":\"/test/Test.java\",\"fileName\":\"Test.java\",\"codePreview\":\"public class RedisConfig\",\"languageType\":\"Java\",\"className\":\"RedisConfig\",\"relevanceScore\":0.8}]";
        
        when(redisCacheService.get(anyString())).thenReturn(cachedJson);

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        verify(codeSnippetRepository, never()).fullTextSearch(anyString());
    }

    @Test
    @DisplayName("Redis缓存未命中查询数据库")
    void testKeywordSearch_CacheMiss() {
        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.fullTextSearch("Redis"))
                .thenReturn(List.of(testSnippet));
        when(searchHistoryRepository.save(any(SearchHistory.class)))
                .thenReturn(new SearchHistory());

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(codeSnippetRepository, times(1)).fullTextSearch("Redis");
    }

    @Test
    @DisplayName("语言筛选正确生效")
    void testKeywordSearch_LanguageFilter() {
        testRequest.setLanguageType("Java");
        testRequest.setKeyword(null);

        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.findByLanguageType("Java"))
                .thenReturn(List.of(testSnippet));

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        verify(codeSnippetRepository, times(1)).findByLanguageType("Java");
    }

    @Test
    @DisplayName("标签筛选正确生效")
    void testKeywordSearch_TagFilter() {
        testRequest.setTag("Redis");
        testRequest.setKeyword(null);

        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.findByTagName("Redis"))
                .thenReturn(List.of(testSnippet));

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        verify(codeSnippetRepository, times(1)).findByTagName("Redis");
    }

    @Test
    @DisplayName("语言和标签组合筛选")
    void testKeywordSearch_LanguageAndTagFilter() {
        testRequest.setLanguageType("Java");
        testRequest.setTag("Redis");
        testRequest.setKeyword(null);

        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.findByLanguageTypeAndTagName("Java", "Redis"))
                .thenReturn(List.of(testSnippet));

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        verify(codeSnippetRepository, times(1)).findByLanguageTypeAndTagName("Java", "Redis");
    }

    @Test
    @DisplayName("精确匹配模式")
    void testKeywordSearch_ExactMatch() {
        testRequest.setExactMatch(true);

        when(redisCacheService.get(anyString())).thenReturn(null);
        when(codeSnippetRepository.findByCodeContentContaining("Redis"))
                .thenReturn(List.of(testSnippet));
        when(codeSnippetRepository.findByFileNameContaining("Redis"))
                .thenReturn(List.of());
        when(codeSnippetRepository.findByClassNameContaining("Redis"))
                .thenReturn(List.of());
        when(searchHistoryRepository.save(any(SearchHistory.class)))
                .thenReturn(new SearchHistory());

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        verify(codeSnippetRepository, times(1)).findByCodeContentContaining("Redis");
    }

    @Test
    @DisplayName("空参数搜索返回空列表")
    void testKeywordSearch_EmptyParams() {
        testRequest.setKeyword(null);
        testRequest.setLanguageType(null);
        testRequest.setTag(null);

        Page<SearchResponse> result = searchService.keywordSearch(testRequest);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("保存搜索历史")
    void testSaveSearchHistory() {
        when(searchHistoryRepository.save(any(SearchHistory.class)))
                .thenReturn(new SearchHistory());

        searchService.saveSearchHistory("Redis", "anonymous");

        verify(searchHistoryRepository, times(1)).save(any(SearchHistory.class));
    }

    @Test
    @DisplayName("获取搜索历史")
    void testGetSearchHistory() {
        SearchHistory history = new SearchHistory();
        history.setKeyword("Redis");
        history.setUserId("anonymous");

        when(searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc("anonymous"))
                .thenReturn(List.of(history));

        List<SearchHistory> result = searchService.getSearchHistory("anonymous");

        assertEquals(1, result.size());
        assertEquals("Redis", result.get(0).getKeyword());
    }

    @Test
    @DisplayName("清空搜索历史")
    void testClearSearchHistory() {
        doNothing().when(searchHistoryRepository).deleteByUserId("anonymous");

        searchService.clearSearchHistory("anonymous");

        verify(searchHistoryRepository, times(1)).deleteByUserId("anonymous");
    }

    @Test
    @DisplayName("获取热门关键词 - 缓存命中")
    void testGetHotKeywords_CacheHit() throws Exception {
        String cachedJson = "[\"Redis\",\"Config\",\"Java\"]";
        when(redisCacheService.get("hot:keywords")).thenReturn(cachedJson);

        List<String> result = searchService.getHotKeywords();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Redis"));
    }

    @Test
    @DisplayName("获取热门关键词 - 缓存未命中")
    void testGetHotKeywords_CacheMiss() {
        when(redisCacheService.get("hot:keywords")).thenReturn(null);
        
        SearchHistory history1 = new SearchHistory();
        history1.setKeyword("Redis");
        SearchHistory history2 = new SearchHistory();
        history2.setKeyword("Redis");
        SearchHistory history3 = new SearchHistory();
        history3.setKeyword("Config");

        when(searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc("anonymous"))
                .thenReturn(List.of(history1, history2, history3));

        List<String> result = searchService.getHotKeywords();

        assertNotNull(result);
        assertTrue(result.contains("Redis"));
    }
}
