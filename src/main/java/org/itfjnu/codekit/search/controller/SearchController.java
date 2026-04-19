package org.itfjnu.codekit.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.model.SearchHistory;
import org.itfjnu.codekit.search.service.SearchService;
import org.itfjnu.codekit.search.service.VectorIndexService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检索模块API接口
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "检索模块", description = "负责代码片段的关键词检索和语义检索")
public class SearchController {

    private final SearchService searchService;
    private final VectorIndexService vectorIndexService;

    /**
     * 关键词检索
     * 请求方式: POST
     * 请求路径: /api/search/keyword
     * 请求体示例: {"keyword":"Redis 连接","languageType":"Java","page":0,"size":10}
     */
    @Operation(summary = "关键词检索", description = "基于 MySQL 全文索引的关键词检索")
    @PostMapping("/keyword")
    public ApiResponse<Page<SearchResponse>> keywordSearch(@Valid @RequestBody SearchRequest request) {
        Page<SearchResponse> result = searchService.keywordSearch(request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "语义检索", description = "基于向量相似度的 RAG 语义检索")
    @PostMapping("/semantic")
    public ApiResponse<Page<SearchResponse>> semanticSearch(@Valid @RequestBody SearchRequest request) {
        Page<SearchResponse> result = searchService.semanticSearch(request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "重建语义索引", description = "为所有代码片段重建向量索引")
    @PostMapping("/semantic/rebuild")
    public ApiResponse<Boolean> rebuildSemanticIndex() {
        return ApiResponse.success(vectorIndexService.rebuildAllEmbeddings());
    }

    /**
     * 获取搜索历史
     * 请求方式: GET
     * 请求路径: /api/search/history
     */
    @Operation(summary = "搜索历史", description = "获取搜索历史记录")
    @GetMapping("/history")
    public ApiResponse<List<SearchHistory>> getSearchHistory() {
        List<SearchHistory> history = searchService.getSearchHistory();
        return ApiResponse.success(history);
    }

    /**
     * 清空搜索历史
     * 请求方式: DELETE
     * 请求路径: /api/search/history
     */
    @Operation(summary = "清空搜索历史", description = "清空指定用户的搜索历史记录")
    @DeleteMapping("/history")
    public ApiResponse<Boolean> clearSearchHistory() {
        Boolean cleared = searchService.clearSearchHistory();
        return ApiResponse.success(cleared);
    }

    /**
     * 获取热门搜索关键词
     * 请求方式: GET
     * 请求路径: /api/search/hot-keywords
     */
    @Operation(summary = "热门搜索关键词", description = "获取热门搜索关键词列表")
    @GetMapping("/hot-keywords")
    public ApiResponse<List<String>> getHotKeywords() {
        List<String> keywords = searchService.getHotKeywords();
        return ApiResponse.success(keywords);
    }
}
