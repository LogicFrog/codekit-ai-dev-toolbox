package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.itfjnu.codekit.search.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeSearchSkillImpl implements Skill {

    private final SearchService searchService;

    @Override
    public String name() {
        return "code-search";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            String keyword = String.valueOf(params.getOrDefault("keyword", "")).trim();
            String mode = String.valueOf(params.getOrDefault("mode", "keyword"));

            SearchRequest req = new SearchRequest();
            req.setKeyword(keyword);
            req.setPage(0);
            req.setSize(5);

            Page<SearchResponse> page;
            if ("semantic".equalsIgnoreCase(mode)) {
                page = searchService.semanticSearch(req);
            } else {
                page = searchService.keywordSearch(req);
            }

            List<SearchResponse> items = page.getContent();

            // 把关键结果写入上下文，供后续 Skill 使用
            context.put("search_total", page.getTotalElements());
            context.put("search_items", items);
            if (!items.isEmpty()) {
                context.put("search_top_preview", items.getFirst().getCodePreview());
                context.put("search_top_language", items.getFirst().getLanguageType());
                context.put("search_top_id", items.getFirst().getId());
            }

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "mode", mode,
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
}
