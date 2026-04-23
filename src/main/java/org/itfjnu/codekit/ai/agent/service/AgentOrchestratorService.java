package org.itfjnu.codekit.ai.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.planner.AgentPlanner;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.ai.agent.skill.SkillRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final AgentPlanner agentPlanner;
    private final SkillRegistry skillRegistry;

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

            // 同时把每步原始结果挂到上下文，便于后续扩展
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

    private String buildSummary(List<AgentTask> tasks, List<SkillResult> results) {
        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.getSuccess())).count();
        long failCount = results.size() -  successCount;

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
