package org.itfjnu.codekit.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.common.cache.RedisCacheService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.model.SearchHistory;
import org.itfjnu.codekit.search.repository.SearchHistoryRepository;
import org.itfjnu.codekit.search.service.SearchService;
import org.itfjnu.codekit.search.service.support.SearchQueryExecutor;
import org.itfjnu.codekit.search.service.support.SearchResponseAssembler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 检索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;
    private final SearchQueryExecutor searchQueryExecutor;
    private final SearchResponseAssembler searchResponseAssembler;

    @Override
    public Page<SearchResponse> keywordSearch(SearchRequest request) {
        String keyword = request.getKeyword();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        Pageable pageable = buildPageable(request);

        log.info("执行关键词检索，关键词: {}, 精确匹配: {}, 语言: {}, 标签: {}",
                keyword, request.getExactMatch(), request.getLanguageType(), request.getTag());

        if (!hasKeyword && request.getLanguageType() == null && request.getTag() == null) {
            log.warn("检索参数为空，请至少提供关键词、语言或标签之一");
            return emptyPage(pageable);
        }

        String cacheKey = buildCacheKey(request);
        List<SearchResponse> cachedResponses = readCachedResponses(cacheKey);
        if (cachedResponses != null) {
            log.info("缓存命中，Key: {}", cacheKey);
            return paginate(cachedResponses, pageable);
        }

        log.info("缓存未命中，查询数据库");
        List<CodeSnippet> snippets = searchQueryExecutor.loadSnippetsByRequest(request);
        List<SearchResponse> responses = searchResponseAssembler.assemble(snippets, request.getKeyword());

        cacheResponses(cacheKey, responses);

        if (hasKeyword) {
            saveSearchHistory(request.getKeyword());
        }

        return paginate(responses, pageable);
    }

    private String buildCacheKey(SearchRequest request) {
        StringBuilder keyBuilder = new StringBuilder("search:keyword:");
        keyBuilder.append(request.getKeyword());
        
        if (request.getLanguageType() != null) {
            keyBuilder.append(":language:").append(request.getLanguageType());
        } else {
            keyBuilder.append(":language:");
        }
        
        if (request.getTag() != null) {
            keyBuilder.append(":tag:").append(request.getTag());
        } else {
            keyBuilder.append(":tag:");
        }
        
        keyBuilder.append(":exact:").append(request.getExactMatch());
        
        return keyBuilder.toString();
    }

    private Pageable buildPageable(SearchRequest request) {
        return PageRequest.of(request.getPage(), request.getSize());
    }

    private Page<SearchResponse> emptyPage(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    private List<SearchResponse> readCachedResponses(String cacheKey) {
        try {
            Object cachedResult = redisCacheService.get(cacheKey);
            if (cachedResult == null) {
                return null;
            }
            return objectMapper.readValue((String) cachedResult, new TypeReference<List<SearchResponse>>() {});
        } catch (Exception e) {
            log.warn("缓存读取失败，将查询数据库: {}", e.getMessage());
            return null;
        }
    }

    private void cacheResponses(String cacheKey, List<SearchResponse> responses) {
        try {
            String json = objectMapper.writeValueAsString(responses);
            redisCacheService.set(cacheKey, json, 10, TimeUnit.MINUTES);
            log.info("缓存已设置，Key: {}, 过期时间: 10 分钟", cacheKey);
        } catch (Exception e) {
            log.warn("缓存写入失败: {}", e.getMessage());
        }
    }

    private Page<SearchResponse> paginate(List<SearchResponse> responses, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int total = responses.size();
        if (start >= total) {
            return emptyPage(pageable);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(responses.subList(start, end), pageable, total);
    }

    @Override
    public Page<SearchResponse> semanticSearch(SearchRequest request) {
        log.info("执行语义检索，关键词: {}", request.getKeyword());
        throw new BusinessException(ErrorCode.FEATURE_NOT_IMPLEMENTED, "语义检索功能尚未实现，请使用关键词检索");
    }

    @Override
    public void saveSearchHistory(String keyword) {
        SearchHistory history = new SearchHistory();
        history.setKeyword(keyword);
        history.setSearchType(0);
        searchHistoryRepository.save(history);
        log.info("保存搜索历史: {}", keyword);
    }

    @Override
    public List<SearchHistory> getSearchHistory() {
        return searchHistoryRepository.findTop10ByOrderBySearchTimeDesc();
    }

    @Override
    public void clearSearchHistory() {
        searchHistoryRepository.deleteAllBy();
        log.info("清空搜索历史");
    }

    @Override
    public List<String> getHotKeywords() {
        String cacheKey = "hot:keywords";
        String cachedResult = redisCacheService.get(cacheKey);
        
        if (cachedResult != null) {
            try {
                log.info("热门关键词缓存命中");
                return objectMapper.readValue(cachedResult, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("热门关键词缓存读取失败: {}", e.getMessage());
            }
        }
        
        log.info("热门关键词缓存未命中，从搜索历史聚合");
        List<SearchHistory> recentHistory = searchHistoryRepository.findTop10ByOrderBySearchTimeDesc();
        Map<String, Long> keywordCount = recentHistory.stream()
            .collect(Collectors.groupingBy(SearchHistory::getKeyword, Collectors.counting()));
        
        List<String> hotKeywords = keywordCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (hotKeywords.isEmpty()) {
            hotKeywords = List.of("Redis", "分页", "连接池", "配置", "工具类");
        }
        
        try {
            String json = objectMapper.writeValueAsString(hotKeywords);
            redisCacheService.set(cacheKey, json, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("热门关键词缓存写入失败: {}", e.getMessage());
        }
        
        return hotKeywords;
    }

}
