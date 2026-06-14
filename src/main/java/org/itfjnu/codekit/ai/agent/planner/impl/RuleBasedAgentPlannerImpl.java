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
        boolean needVersion = containsAny(lower, "版本", "历史", "version");
        boolean needOptimize = containsAny(lower, "优化", "重构", "improve", "optimize");
        boolean needCompare = containsAny(lower, "对比", "差异", "diff", "compare");
        boolean needRag = containsAny(lower, "语义检索", "rag", "向量检索", "语义召回", "知识检索");

        if (needRag) {
            Map<String, Object> params = new HashMap<>();
            params.put("query", extractSearchKeyword(text));
            params.put("topK", 5);
            params.put("minScore", 0.0);
            params.put("includeCode", true);

            tasks.add(AgentTask.builder()
                    .taskName("RAG 语义检索")
                    .skillName("rag_retrieve")
                    .params(params)
                    .build());
        } else if (needSearch) {
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

        if (needExplain && needOptimize) {
            // 解释+优化合并：一次 LLM 调用同时完成解释和优化，减少等待时间
            Map<String, Object> params = new HashMap<>();
            params.put("optimizeType", "explain_and_optimize");
            params.put("question", text);

            tasks.add(AgentTask.builder()
                    .taskName("代码解释与优化分析")
                    .skillName("code_optimize")
                    .params(params)
                    .build());
        } else if (needExplain) {
            Map<String, Object> params = new HashMap<>();
            params.put("question", text);

            tasks.add(AgentTask.builder()
                    .taskName("解释与风险分析")
                    .skillName("ai_explain")
                    .params(params)
                    .build());
        } else if (needOptimize) {
            Map<String, Object> params = new HashMap<>();
            String optimizeType = extractOptimizeType(text);
            params.put("optimizeType", optimizeType);
            params.put("question", text);

            // 先搜索相关代码（除非用户明确有不需要搜索的关键词，或前面已有搜索任务）
            if (!containsAny(text, "直接优化") && tasks.stream().noneMatch(t -> "code_search".equals(t.getSkillName()))) {
                Map<String, Object> searchParams = new HashMap<>();
                String keyword = extractSearchKeyword(text);
                searchParams.put("keyword", keyword);
                searchParams.put("fallbackKeyword", text);
                searchParams.put("mode", "semantic");
                
                tasks.add(AgentTask.builder()
                        .taskName("搜索代码片段")
                        .skillName("code_search")
                        .params(searchParams)
                        .build());
            }

            tasks.add(AgentTask.builder()
                    .taskName("代码优化")
                    .skillName("code_optimize")
                    .params(params)
                    .build());
        }

        if (needCompare) {
            Long snippetId = extractSnippetId(text);
            
            // 如果没有 snippetId，先搜索代码
            if (snippetId == null) {
                Map<String, Object> searchParams = new HashMap<>();
                String keyword = extractSearchKeyword(text);
                searchParams.put("keyword", keyword);
                searchParams.put("fallbackKeyword", text);
                searchParams.put("mode", "keyword"); // 文件名用关键词检索更准
                
                tasks.add(AgentTask.builder()
                        .taskName("搜索代码片段")
                        .skillName("code_search")
                        .params(searchParams)
                        .build());
            }
            
            // 然后对比版本（如果有 snippetId 直接用，否则让 Skill 从上下文找 search_top_id）
            Map<String, Object> compareParams = new HashMap<>();
            if (snippetId != null) {
                compareParams.put("snippetId", snippetId);
            }

            tasks.add(AgentTask.builder()
                    .taskName("版本对比")
                    .skillName("git_compare")
                    .params(compareParams)
                    .build());
        } else if (needVersion) {
            Long snippetId = extractSnippetId(text);
            
            // 如果没有 snippetId，先搜索代码
            if (snippetId == null) {
                Map<String, Object> searchParams = new HashMap<>();
                String keyword = extractSearchKeyword(text);
                searchParams.put("keyword", keyword);
                searchParams.put("fallbackKeyword", text);
                searchParams.put("mode", "keyword");
                
                tasks.add(AgentTask.builder()
                        .taskName("搜索代码片段")
                        .skillName("code_search")
                        .params(searchParams)
                        .build());
            }
            
            // 然后查询版本
            Map<String, Object> params = new HashMap<>();
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

        // 先尝试提取文件名（如 "CategoryService.java"）
        Pattern fileNamePattern = Pattern.compile("([a-zA-Z0-9_]+\\.[a-zA-Z0-9]+)");
        Matcher fileNameMatcher = fileNamePattern.matcher(instruction);
        if (fileNameMatcher.find()) {
            return fileNameMatcher.group(1);
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

    private String extractOptimizeType(String text) {
        String lower = text.toLowerCase();
        if (containsAny(lower, "性能", "performance")) {
            return "performance";
        }
        if (containsAny(lower, "可读性", "readability")) {
            return "readability";
        }
        if (containsAny(lower, "bug", "修复", "bugfix")) {
            return "bugfix";
        }
        return "all";
    }
}
