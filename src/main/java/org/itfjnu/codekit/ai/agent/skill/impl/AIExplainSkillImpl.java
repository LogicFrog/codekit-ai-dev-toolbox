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
public class AIExplainSkillImpl implements Skill {

    private final AIService aiService;
    private final CodeSnippetRepository codeSnippetRepository;

    @Override
    public String name() {
        return "ai_explain";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            // 优先使用 params；如果没传，从上下文拿 code_search 的完整代码
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
            // 双保险：若仍为空但有 snippetId，则回源数据库拿完整代码
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

            String question = String.valueOf(params.getOrDefault("question", "请解释这段代码的作用和风险"));

            AIChatRequest req = new AIChatRequest();
            req.setCode(code);
            req.setLanguageType(language);
            req.setQuestion(question);

            AIChatResponse resp = aiService.explain(req);

            List<String> suggestions = resp.getSuggestions() == null ? List.of() : resp.getSuggestions();
            context.put("explain_answer", resp.getAnswer());
            context.put("explain_suggestions", suggestions);

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "answer", resp.getAnswer() == null ? "" : resp.getAnswer(),
                            "suggestions", suggestions
                    ))
                    .build();


        } catch (Exception e) {
            log.error("AIExplainSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }


    }
}
