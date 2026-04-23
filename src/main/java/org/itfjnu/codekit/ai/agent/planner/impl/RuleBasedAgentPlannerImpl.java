package org.itfjnu.codekit.ai.agent.planner.impl;

import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.itfjnu.codekit.ai.agent.planner.AgentPlanner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RuleBasedAgentPlannerImpl implements AgentPlanner {

    private static final Pattern SNIPPET_ID_PATTERN = Pattern.compile("snippetId\\s*[=:]\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}_]+");
    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "帮我", "请", "一下", "帮忙", "搜索", "检索", "查找", "找", "语义", "semantic", "search",
            "代码", "并", "然后", "解释", "分析", "风险", "的", "和", "请问", "查看", "看看", "关于"
    );


    @Override
    public List<AgentTask> plan(String instruction) {
        String text = instruction == null ? "" : instruction.trim();
        String lower = text.toLowerCase();

        List<AgentTask> tasks = new ArrayList<AgentTask>();

        boolean needSearch = containsAny(lower, "找", "搜索", "检索", "search", "语义");
        boolean needExplain = containsAny(lower, "解释", "分析", "风险", "explain");
        boolean needVersion = containsAny(lower, "版本", "历史", "version", "diff");

        if (needSearch) {
            Map<String, Object> params = new HashMap<>();
            String keyword = extractSearchKeyword(text);
            params.put("keyword", keyword);
            params.put("fallbackKeyword", text);
            // Agent 默认优先语义检索；如果语义无结果或关键词为空，再在 Skill 内降级到关键词检索
            params.put("mode", "semantic");

            tasks.add(AgentTask.builder()
                            .taskName("检索相关代码")
                            .skillName("code_search")
                            .params(params)
                            .build());
        }

        if (needExplain) {
            Map<String, Object> params = new HashMap<>();
            params.put("question", text);

            tasks.add(AgentTask.builder()
                            .taskName("解释与风险分析")
                            .skillName("ai_explain")
                            .params(params)
                            .build());
        }

        if (needVersion) {
            Map<String, Object> params = new HashMap<>();
            Long snippetId = extractSnippetId(text);
            if (snippetId != null) {
                params.put("snippetId", snippetId);
            }

            tasks.add(AgentTask.builder()
                    .taskName("查询版本列表")
                    .skillName("version_list")
                    .params(params)
                    .build());
        }
        // 兜底
        if (tasks.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("question", text.isEmpty() ? "请给我一个代码优化建议" : text);
            tasks.add(AgentTask.builder()
                    .taskName("通用问题解释")
                    .skillName("ai_explain")
                    .params(params)
                    .build());
        }
        return tasks;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private Long extractSnippetId(String text) {
        Matcher matcher = SNIPPET_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private String extractSearchKeyword(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return "";
        }

        String withoutSnippetId = SNIPPET_ID_PATTERN.matcher(instruction).replaceAll(" ");
        String normalized = TOKEN_SPLIT_PATTERN.matcher(withoutSnippetId).replaceAll(" ").trim();
        if (normalized.isEmpty()) {
            return "";
        }

        LinkedHashSet<String> keywords = Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .filter(token -> !SEARCH_STOP_WORDS.contains(token.toLowerCase()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (keywords.isEmpty()) {
            return "";
        }

        return keywords.stream().limit(4).collect(Collectors.joining(" "));
    }
}
