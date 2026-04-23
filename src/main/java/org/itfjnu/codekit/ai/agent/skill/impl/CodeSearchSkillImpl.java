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

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeSearchSkillImpl implements Skill {

    private final SearchService searchService;
    private final CodeSnippetRepository codeSnippetRepository;

    @Override
    public String name() {
        return "code_search";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            String keyword = String.valueOf(params.getOrDefault("keyword", "")).trim();
            String fallbackKeyword = String.valueOf(params.getOrDefault("fallbackKeyword", "")).trim();
            String mode = String.valueOf(params.getOrDefault("mode", "semantic"));
            String effectiveKeyword = keyword;
            String effectiveMode = mode;

            // 关键词抽取为空时，自动降级为关键词检索，并使用原始指令作为兜底关键词
            if (effectiveKeyword.isEmpty()) {
                effectiveKeyword = fallbackKeyword;
                effectiveMode = "keyword";
            }

            if (effectiveKeyword.isEmpty()) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("检索关键词为空")
                        .build();
            }

            SearchRequest req = new SearchRequest();
            req.setKeyword(effectiveKeyword);
            req.setPage(0);
            req.setSize(5);

            Page<SearchResponse> page = doSearch(req, effectiveMode);
            // 默认优先语义检索：若语义结果为空，自动降级到关键词检索
            if ("semantic".equalsIgnoreCase(effectiveMode) && page.getContent().isEmpty()) {
                page = doSearch(req, "keyword");
                effectiveMode = "keyword";
            }

            List<SearchResponse> items = page.getContent();

            // 把关键结果写入上下文，供后续 Skill 使用
            context.put("search_total", page.getTotalElements());
            context.put("search_items", items);
            context.put("search_keyword", effectiveKeyword);
            context.put("search_mode", effectiveMode);
            if (!items.isEmpty()) {
                context.put("search_top_preview", items.getFirst().getCodePreview());
                context.put("search_top_language", items.getFirst().getLanguageType());
                context.put("search_top_id", items.getFirst().getId());
                Optional<CodeSnippet> topSnippet = codeSnippetRepository.findById(items.getFirst().getId());
                topSnippet.map(CodeSnippet::getCodeContent)
                        .ifPresent(code -> context.put("search_top_code", code));
            }

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "mode", effectiveMode,
                            "keyword", effectiveKeyword,
                            "total", page.getTotalElements(),
                            "items", items
                    ))
                    .build();
        } catch (Exception e) {
            log.error("CodeSearchSkill执行失败",e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }

    private Page<SearchResponse> doSearch(SearchRequest req, String mode) {
        if ("semantic".equalsIgnoreCase(mode)) {
            return searchService.semanticSearch(req);
        }
        return searchService.keywordSearch(req);
    }
}
