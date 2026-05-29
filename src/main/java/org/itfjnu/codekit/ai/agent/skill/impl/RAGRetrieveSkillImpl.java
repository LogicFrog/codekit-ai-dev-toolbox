package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RAGRetrieveSkillImpl implements Skill {

    private final SearchService searchService;
    private final CodeSnippetRepository codeSnippetRepository;

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.0;

    @Override
    public String name() {
        return "rag_retrieve";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            String query = extractString(params, "query", context);
            if (query.isEmpty()) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("RAG 检索 query 参数为空，请提供自然语言查询语句")
                        .build();
            }

            int topK = extractInt(params, "topK", DEFAULT_TOP_K);
            double minScore = extractDouble(params, "minScore", DEFAULT_MIN_SCORE);
            String languageType = extractString(params, "languageType", null);
            String tag = extractString(params, "tag", null);
            boolean includeCode = extractBoolean(params, "includeCode", true);

            SearchRequest req = new SearchRequest();
            req.setKeyword(query);
            req.setPage(0);
            req.setSize(Math.max(1, Math.min(topK, 50)));
            if (languageType != null && !languageType.isBlank()) {
                req.setLanguageType(languageType.trim());
            }
            if (tag != null && !tag.isBlank()) {
                req.setTag(tag.trim());
            }

            Page<SearchResponse> page = searchService.semanticSearch(req);
            List<SearchResponse> items = page.getContent();

            if (minScore > 0.0) {
                items = items.stream()
                        .filter(r -> r.getRelevanceScore() != null && r.getRelevanceScore() >= minScore)
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> enrichedItems = new ArrayList<>();
            for (SearchResponse item : items) {
                Map<String, Object> enriched = new LinkedHashMap<>();
                enriched.put("id", item.getId());
                enriched.put("fileName", item.getFileName());
                enriched.put("filePath", item.getFilePath());
                enriched.put("languageType", item.getLanguageType());
                enriched.put("codePreview", item.getCodePreview());
                enriched.put("relevanceScore", item.getRelevanceScore());
                if (includeCode) {
                    Optional<CodeSnippet> snippet = codeSnippetRepository.findById(item.getId());
                    snippet.ifPresent(s -> enriched.put("fullCode", s.getCodeContent()));
                }
                enrichedItems.add(enriched);
            }

            context.put("retrieve_query", query);
            context.put("retrieve_items", enrichedItems);
            context.put("retrieve_total", items.size());
            context.put("retrieve_topK", topK);
            context.put("retrieve_minScore", minScore);

            if (!items.isEmpty()) {
                SearchResponse top = items.get(0);
                context.put("search_top_preview", top.getCodePreview());
                context.put("search_top_language", top.getLanguageType());
                context.put("search_top_id", top.getId());
                if (includeCode) {
                    Optional<CodeSnippet> topSnippet = codeSnippetRepository.findById(top.getId());
                    topSnippet.map(CodeSnippet::getCodeContent)
                            .ifPresent(code -> context.put("search_top_code", code));
                }
            }

            if (items.isEmpty()) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error(String.format("RAG 语义检索无结果。query=%s, minScore=%.2f, language=%s, tag=%s",
                                query, minScore,
                                languageType != null ? languageType : "不限",
                                tag != null ? tag : "不限"))
                        .data(Map.of(
                                "query", query,
                                "total", 0,
                                "topK", topK,
                                "minScore", minScore
                        ))
                        .build();
            }

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "query", query,
                            "total", items.size(),
                            "topK", topK,
                            "minScore", minScore,
                            "items", enrichedItems,
                            "topScore", items.get(0).getRelevanceScore()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("RAGRetrieveSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }

    private String extractString(Map<String, Object> params, String key, Map<String, Object> fallbackContext) {
        Object val = params.get(key);
        if (val != null) {
            String s = String.valueOf(val).trim();
            if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        if (fallbackContext != null && fallbackContext != params) {
            Object ctxVal = fallbackContext.get(key);
            if (ctxVal != null) {
                String s = String.valueOf(ctxVal).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }

    private int extractInt(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val != null) {
            try {
                return Integer.parseInt(String.valueOf(val).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private double extractDouble(Map<String, Object> params, String key, double defaultValue) {
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val != null) {
            try {
                return Double.parseDouble(String.valueOf(val).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private boolean extractBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object val = params.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val != null) {
            String s = String.valueOf(val).trim().toLowerCase();
            if ("true".equals(s) || "false".equals(s)) {
                return Boolean.parseBoolean(s);
            }
        }
        return defaultValue;
    }
}
