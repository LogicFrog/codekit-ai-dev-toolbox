package org.itfjnu.codekit.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.common.cache.RedisCacheService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.model.SearchHistory;
import org.itfjnu.codekit.search.repository.SearchHistoryRepository;
import org.itfjnu.codekit.search.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private final CodeSnippetRepository codeSnippetRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<SearchResponse> keywordSearch(SearchRequest request) {
        String keyword = request.getKeyword();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        log.info("执行关键词检索，关键词: {}, 精确匹配: {}, 语言: {}, 标签: {}",
                keyword, request.getExactMatch(), request.getLanguageType(), request.getTag());

        if (!hasKeyword && request.getLanguageType() == null && request.getTag() == null) {
            log.warn("检索参数为空，请至少提供关键词、语言或标签之一");
            return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        }

        String cacheKey = buildCacheKey(request);

        try {
            Object cachedResult = redisCacheService.get(cacheKey);
            if (cachedResult != null) {
                log.info("缓存命中，Key: {}", cacheKey);
                String json = (String) cachedResult;
                List<SearchResponse> cachedResponses = objectMapper.readValue(json, new TypeReference<List<SearchResponse>>() {});

                Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
                int start = (int) pageable.getOffset();
                int total = cachedResponses.size();

                if (start >= total) {
                    return new PageImpl<>(List.of(), pageable, 0);
                }

                int end = Math.min(start + pageable.getPageSize(), total);
                List<SearchResponse> pageContent = cachedResponses.subList(start, end);
                return new PageImpl<>(pageContent, pageable, total);
            }
        } catch (Exception e) {
            log.warn("缓存读取失败，将查询数据库: {}", e.getMessage());
        }

        log.info("缓存未命中，查询数据库");
        List<CodeSnippet> snippets;

        if (!hasKeyword) {
            if (request.getLanguageType() != null && request.getTag() != null) {
                snippets = codeSnippetRepository.findByLanguageTypeAndTagName(request.getLanguageType(), request.getTag());
            } else if (request.getLanguageType() != null) {
                snippets = codeSnippetRepository.findByLanguageType(request.getLanguageType());
            } else if (request.getTag() != null) {
                snippets = codeSnippetRepository.findByTagName(request.getTag());
            } else {
                snippets = new ArrayList<>();
            }
        } else if (request.getExactMatch()) {
            List<CodeSnippet> contentSnippets = new ArrayList<>();
            List<CodeSnippet> fileNameSnippets = new ArrayList<>();
            List<CodeSnippet> classNameSnippets = new ArrayList<>();
            
            if (request.getLanguageType() != null && request.getTag() != null) {
                contentSnippets = codeSnippetRepository.findByLanguageTypeAndTagAndCodeContentContaining(
                        request.getLanguageType(),
                        request.getTag(),
                        request.getKeyword()
                );
                fileNameSnippets = codeSnippetRepository.findByLanguageTypeAndTagAndFileNameContaining(
                        request.getLanguageType(),
                        request.getTag(),
                        request.getKeyword()
                );
                classNameSnippets = codeSnippetRepository.findByLanguageTypeAndTagAndClassNameContaining(
                        request.getLanguageType(),
                        request.getTag(),
                        request.getKeyword()
                );
            } else if (request.getLanguageType() != null) {
                contentSnippets = codeSnippetRepository.findByLanguageTypeAndCodeContentContaining(
                        request.getLanguageType(),
                        request.getKeyword()
                );
                fileNameSnippets = codeSnippetRepository.findByLanguageTypeAndFileNameContaining(
                        request.getLanguageType(),
                        request.getKeyword()
                );
                classNameSnippets = codeSnippetRepository.findByLanguageTypeAndClassNameContaining(
                        request.getLanguageType(),
                        request.getKeyword()
                );
            } else if (request.getTag() != null) {
                contentSnippets = codeSnippetRepository.findByTagAndCodeContentContaining(
                        request.getTag(),
                        request.getKeyword()
                );
                fileNameSnippets = codeSnippetRepository.findByTagAndFileNameContaining(
                        request.getTag(),
                        request.getKeyword()
                );
                classNameSnippets = codeSnippetRepository.findByTagAndClassNameContaining(
                        request.getTag(),
                        request.getKeyword()
                );
            } else {
                contentSnippets = codeSnippetRepository.findByCodeContentContaining(request.getKeyword());
                fileNameSnippets = codeSnippetRepository.findByFileNameContaining(request.getKeyword());
                classNameSnippets = codeSnippetRepository.findByClassNameContaining(request.getKeyword());
            }
            
            snippets = new ArrayList<>();
            snippets.addAll(contentSnippets);
            snippets.addAll(fileNameSnippets);
            snippets.addAll(classNameSnippets);
            
            snippets = snippets.stream()
                .distinct()
                .collect(Collectors.toList());
        } else {
            if (request.getLanguageType() != null && request.getTag() != null) {
                snippets = codeSnippetRepository.fullTextSearchByLanguageAndTag(
                        request.getKeyword(),
                        request.getLanguageType(),
                        request.getTag()
                );
            } else if (request.getLanguageType() != null) {
                snippets = codeSnippetRepository.fullTextSearchByLanguage(
                        request.getKeyword(),
                        request.getLanguageType()
                );
            } else if (request.getTag() != null) {
                snippets = codeSnippetRepository.fullTextSearchByTag(
                        request.getKeyword(),
                        request.getTag()
                );
            } else {
                snippets = codeSnippetRepository.fullTextSearch(request.getKeyword());
            }
        }

        List<SearchResponse> responses = snippets.stream()
                .map(snippet -> convertToSearchResponse(snippet, request.getKeyword()))
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .collect(Collectors.toList());

        try {
            String json = objectMapper.writeValueAsString(responses);
            redisCacheService.set(cacheKey, json, 10, TimeUnit.MINUTES);
            log.info("缓存已设置，Key: {}, 过期时间: 10 分钟", cacheKey);
        } catch (Exception e) {
            log.warn("缓存写入失败: {}", e.getMessage());
        }

        if (hasKeyword) {
            saveSearchHistory(request.getKeyword(), "anonymous");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        int start = (int) pageable.getOffset();
        int total = responses.size();
        
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        int end = Math.min(start + pageable.getPageSize(), total);
        List<SearchResponse> pageContent = responses.subList(start, end);
        return new PageImpl<>(pageContent, pageable, total);
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

    @Override
    public Page<SearchResponse> semanticSearch(SearchRequest request) {
        log.info("执行语义检索，关键词: {}", request.getKeyword());
        throw new BusinessException(ErrorCode.FEATURE_NOT_IMPLEMENTED, "语义检索功能尚未实现，请使用关键词检索");
    }

    @Override
    public void saveSearchHistory(String keyword, String userId) {
        SearchHistory history = new SearchHistory();
        history.setKeyword(keyword);
        history.setSearchType(0);
        history.setUserId(userId);
        searchHistoryRepository.save(history);
        log.info("保存搜索历史: {}, 用户: {}", keyword, userId);
    }

    @Override
    public List<SearchHistory> getSearchHistory(String userId) {
        return searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(userId);
    }

    @Override
    public void clearSearchHistory(String userId) {
        searchHistoryRepository.deleteByUserId(userId);
        log.info("清空搜索历史: {}", userId);
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
        List<SearchHistory> recentHistory = searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc("anonymous");
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

    /**
     * 将 CodeSnippet 转换为 SearchResponse
     */
    private SearchResponse convertToSearchResponse(CodeSnippet snippet, String keyword) {
        String codePreview = "";
        String highlight = "";

        if (snippet.getCodeContent() != null) {
            String content = snippet.getCodeContent();
            if (keyword != null && !keyword.trim().isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                String lowerContent = content.toLowerCase();

                int keywordIndex = lowerContent.indexOf(lowerKeyword);

                if (keywordIndex >= 0) {
                    int start = Math.max(0, keywordIndex - 100);
                    int end = Math.min(content.length(), keywordIndex + keyword.length() + 100);
                    codePreview = content.substring(start, end);

                    highlight = content.substring(keywordIndex, keywordIndex + keyword.length());
                } else {
                    codePreview = content.substring(0, Math.min(200, content.length()));
                }
            } else {
                codePreview = content.substring(0, Math.min(200, content.length()));
            }
        }

        return SearchResponse.builder()
                .id(snippet.getId())
                .filePath(snippet.getFilePath())
                .fileName(snippet.getFileName())
                .codePreview(codePreview)
                .highlight(highlight)
                .languageType(snippet.getLanguageType())
                .className(snippet.getClassName())
                .packageName(snippet.getPackageName())
                .tags(snippet.getTags())
                .relevanceScore(calculateRelevanceScore(snippet, keyword))
                .createTime(snippet.getCreateTime())
                .build();
    }

    /**
     *  计算相关性分数
     */
    private double calculateRelevanceScore(CodeSnippet snippet, String keyword) {
        double score = 0.0;

        if (keyword == null || keyword.trim().isEmpty()) {
            return 0.0;
        }

        String lowerKeyword = keyword.toLowerCase();  // 关键词统一转化为小写

        if (snippet.getFileName() != null &&
                snippet.getFileName().toLowerCase().contains(lowerKeyword)) {
            score += 1.0;
        }

        if (snippet.getClassName() != null &&
                snippet.getClassName().toLowerCase().contains(lowerKeyword)) {
            score += 0.8;
        }

        if (snippet.getPackageName() != null &&
                snippet.getPackageName().toLowerCase().contains(lowerKeyword)) {
            score += 0.6;
        }

        if (snippet.getCodeContent() != null &&
                snippet.getCodeContent().toLowerCase().contains(lowerKeyword)) {
            score += 0.4;
        }

        if (snippet.getTags() != null &&
                snippet.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword))) {
            score += 0.5;
        }

        return Math.min(score, 1.0);
    }
}
