package org.itfjnu.codekit.ai.agent.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.planner.AgentPlanner;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.ai.agent.skill.SkillRegistry;
import org.itfjnu.codekit.ai.agent.service.AgentOrchestratorService;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

    private final AgentPlanner agentPlanner;
    private final SkillRegistry skillRegistry;
    private final AIService aiService;
    private final CodeSnippetRepository codeSnippetRepository;

    private static final Set<String> AI_SKILL_NAMES = Set.of("ai_explain", "code_optimize");

    @Override
    public AgentExecuteResponse execute(String instruction) {
        List<AgentTask> tasks = agentPlanner.plan(instruction);
        List<SkillResult> results = new ArrayList<>();
        Map<String, Object> context = new HashMap<>();

        for (AgentTask task : tasks) {
            String skillName = task.getSkillName();
            Skill skill = skillRegistry.findByName(skillName).orElse(null);

            if (skill == null) {
                SkillResult fail = SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(skillName)
                        .error("Skill不存在：" + skillName)
                        .build();
                results.add(fail);
                continue;
            }

            SkillResult result = skill.execute(task.getParams(), context);
            results.add(result);

            context.put("result:" + skillName, result.getData());
        }

        String summary = buildSummary(tasks, results);

        return AgentExecuteResponse.builder()
                .instruction(instruction)
                .tasks(tasks)
                .results(results)
                .summary(summary)
                .build();
    }

    @Override
    public SseEmitter executeStream(String instruction) {
        SseEmitter emitter = new SseEmitter(300000L);

        Thread.startVirtualThread(() -> {
            try {
                List<AgentTask> tasks = agentPlanner.plan(instruction);
                sendEvent(emitter, "plan", Map.of(
                        "tasks", tasks,
                        "count", tasks.size()
                ));

                List<SkillResult> results = new ArrayList<>();
                Map<String, Object> context = new HashMap<>();

                for (int i = 0; i < tasks.size(); i++) {
                    AgentTask task = tasks.get(i);
                    String skillName = task.getSkillName();
                    Map<String, Object> params = task.getParams();

                    sendEvent(emitter, "skill_start", Map.of(
                            "skillName", skillName,
                            "taskName", task.getTaskName(),
                            "index", i,
                            "total", tasks.size()
                    ));

                    SkillResult result;
                    if (AI_SKILL_NAMES.contains(skillName)) {
                        result = executeAIStreamSkill(skillName, params, context, emitter);
                    } else {
                        result = executeNonAISkill(skillName, params, context);
                    }

                    results.add(result);
                    if (Boolean.TRUE.equals(result.getSuccess())) {
                        context.put("result:" + skillName, result.getData());
                    }

                    sendEvent(emitter, "skill_complete", Map.of(
                            "skillName", skillName,
                            "success", result.getSuccess(),
                            "data", result.getData(),
                            "error", result.getError() != null ? result.getError() : ""
                    ));
                }

                String summary = buildSummary(tasks, results);
                sendEvent(emitter, "done", Map.of(
                        "instruction", instruction,
                        "tasks", tasks,
                        "results", results,
                        "summary", summary
                ));
                emitter.complete();
            } catch (Exception e) {
                log.error("Agent 流式执行失败", e);
                try {
                    sendEvent(emitter, "error", Map.of("message", e.getMessage() != null ? e.getMessage() : "未知错误"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private SkillResult executeNonAISkill(String skillName, Map<String, Object> params, Map<String, Object> context) {
        Skill skill = skillRegistry.findByName(skillName).orElse(null);
        if (skill == null) {
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(skillName)
                    .error("Skill不存在：" + skillName)
                    .build();
        }
        try {
            return skill.execute(params, context);
        } catch (Exception e) {
            log.error("Skill [{}] 执行失败", skillName, e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(skillName)
                    .error(e.getMessage())
                    .build();
        }
    }

    private SkillResult executeAIStreamSkill(String skillName, Map<String, Object> params,
                                             Map<String, Object> context, SseEmitter emitter) {
        try {
            AIChatRequest req = buildAIChatRequest(skillName, params, context);
            if (req == null) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(skillName)
                        .error("无法构建AI请求：代码内容为空")
                        .build();
            }

            AIChatResponse resp;
            if ("ai_explain".equals(skillName)) {
                resp = aiService.explainStream(req, chunk ->
                        sendEvent(emitter, "skill_chunk", Map.of("skillName", skillName, "chunk", chunk))
                );
            } else {
                resp = aiService.optimizeStream(req, chunk ->
                        sendEvent(emitter, "skill_chunk", Map.of("skillName", skillName, "chunk", chunk))
                );
            }

            List<String> suggestions = resp.getSuggestions() == null ? List.of() : resp.getSuggestions();
            if ("ai_explain".equals(skillName)) {
                context.put("explain_answer", resp.getAnswer());
                context.put("explain_suggestions", suggestions);
            } else {
                context.put("optimize_answer", resp.getAnswer());
                context.put("optimize_suggestions", suggestions);
            }

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(skillName)
                    .data(Map.of(
                            "answer", resp.getAnswer() == null ? "" : resp.getAnswer(),
                            "suggestions", suggestions
                    ))
                    .build();
        } catch (Exception e) {
            log.error("AI流式Skill [{}] 执行失败", skillName, e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(skillName)
                    .error(e.getMessage())
                    .build();
        }
    }

    private AIChatRequest buildAIChatRequest(String skillName, Map<String, Object> params, Map<String, Object> context) {
        String code = String.valueOf(params.getOrDefault("code", "")).trim();
        if (code.isEmpty()) {
            Object fromSearch = context.get("search_top_code");
            code = fromSearch == null ? "" : String.valueOf(fromSearch);
        }
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
                }
            }
        }

        String language = String.valueOf(params.getOrDefault("languageType", "")).trim();
        if (language.isEmpty()) {
            Object langFromSearch = context.get("search_top_language");
            language = langFromSearch == null ? "Java" : String.valueOf(langFromSearch);
        }

        String question = String.valueOf(params.getOrDefault("question", "请解释这段代码"));

        AIChatRequest req = new AIChatRequest();
        req.setCode(code);
        req.setLanguageType(language);
        req.setQuestion(question);
        return req;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new RuntimeException("SSE 推送失败", e);
        }
    }

    private String buildSummary(List<AgentTask> tasks, List<SkillResult> results) {
        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.getSuccess())).count();
        long failCount = results.size() - successCount;

        StringBuilder sb = new StringBuilder();
        sb.append("本次 Agent 共执行 ").append(tasks.size()).append(" 个任务，")
                .append("成功 ").append(successCount).append(" 个，失败 ").append(failCount).append(" 个。\n");

        for (int i = 0; i < tasks.size(); i++) {
            AgentTask task = tasks.get(i);
            SkillResult result = i < results.size() ? results.get(i) : null;
            sb.append(i + 1).append(". ")
                    .append(task.getTaskName())
                    .append(" [").append(task.getSkillName()).append("] -> ")
                    .append(result == null ? "无结果" : (Boolean.TRUE.equals(result.getSuccess()) ? "成功" : "失败"))
                    .append("\n");
        }

        return sb.toString();
    }
}