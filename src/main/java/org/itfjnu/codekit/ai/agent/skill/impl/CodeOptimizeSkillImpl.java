package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeOptimizeSkillImpl implements Skill {

    private final AIService aiService;
    private final CodeSnippetRepository codeSnippetRepository;

    @Override
    public String name() {
        return "code_optimize";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            String code = String.valueOf(params.getOrDefault("code", "")).trim();
            if (code.isEmpty()) {
                Object fromSearch = context.get("search_top_code");
                code = fromSearch == null ? "" : String.valueOf(fromSearch);
            }

            // 兼容旧上下文：没有完整代码时再退回 preview
            if (code.isEmpty()) {
                Object preview = context.get("search_top_preview");
                code = preview == null ? "" : String.valueOf(preview);
            }

            if (code.isEmpty()) {
                Object snippetIdObj = context.get("search_top_id");
                if (snippetIdObj != null) {
                    try {
                        Long snippetId = Long.valueOf(String.valueOf(snippetIdObj));
                        Optional<CodeSnippet> snippet = codeSnippetRepository.findById(snippetId);
                        code = snippet.map(CodeSnippet::getCodeContent).orElse("");
                    } catch (Exception ignored) {
                        log.warn("从上下文 snippetId 获取完整代码失败: {}", snippetIdObj);
                    }
                }
            }

            String language = String.valueOf(params.getOrDefault("languageType", "")).trim();
            if (language.isEmpty()) {
                Object langFromSearch = context.get("search_top_language");
                language = langFromSearch == null ? "Java" : String.valueOf(langFromSearch);
            }

            String optimizeType = String.valueOf(params.getOrDefault("optimizeType", "all")).trim();
            String question = String.valueOf(params.getOrDefault("question", buildOptimizeQuestion(optimizeType)));

            AIChatRequest req = new AIChatRequest();
            req.setCode(code);
            req.setLanguageType(language);
            req.setQuestion(question);

            AIChatResponse resp = aiService.optimize(req);

            List<String> suggestions = resp.getSuggestions() == null ? List.of() : resp.getSuggestions();
            context.put("optimize_answer", resp.getAnswer());
            context.put("optimize_suggestions", suggestions);

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "answer", resp.getAnswer() == null ? "" : resp.getAnswer(),
                            "suggestions", suggestions,
                            "optimizeType", optimizeType
                    ))
                    .build();

        } catch (Exception e) {
            log.error("CodeOptimizeSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }

    private String buildOptimizeQuestion(String optimizeType) {
        return switch (optimizeType.toLowerCase()) {
            case "performance" -> "请优化这段代码的性能，减少时间复杂度和空间复杂度，给出具体的优化方案和优化后的代码";
            case "readability" -> "请优化这段代码的可读性，重构变量命名、提取方法、简化逻辑，让代码更易理解";
            case "bugfix" -> "请检查这段代码中可能存在的 Bug（空指针、数组越界、逻辑错误等），给出修复方案和修复后的代码";
            default -> "请全面优化这段代码，从性能、可读性、安全性三个方面进行优化，给出具体的优化建议和优化后的代码";
        };
    }
}
