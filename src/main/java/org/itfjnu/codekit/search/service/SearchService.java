package org.itfjnu.codekit.search.service;

import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.model.SearchHistory;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SearchService {
    /**
     * 关键词检索
     */
    Page<SearchResponse> keywordSearch(SearchRequest request);

    /**
     * 语义检索
     */
    Page<SearchResponse> semanticSearch(SearchRequest request);

    /**
     * 检索历史记录
     */
    void saveSearchHistory(String keyword);

    /**
     * 获取搜索历史
     */
    List<SearchHistory> getSearchHistory();

    /**
     * 清空搜索历史
     */
    void clearSearchHistory();

    /**
     * 获取热门搜索关键词
     */
    List<String> getHotKeywords();
}
