package org.itfjnu.codekit.ai.agent.service;

import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.planner.AgentPlanner;
import org.itfjnu.codekit.ai.agent.service.impl.AgentOrchestratorServiceImpl;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.ai.agent.skill.SkillRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorServiceTest {

    @Mock
    private AgentPlanner agentPlanner;

    @Mock
    private SkillRegistry skillRegistry;

    @InjectMocks
    private AgentOrchestratorServiceImpl orchestrator;

    @Test
    @DisplayName("单任务执行成功返回正确的响应")
    void execute_SingleTaskSuccess_ReturnsCorrectResponse() {
        AgentTask task = AgentTask.builder()
                .taskName("搜索代码")
                .skillName("code_search")
                .params(Map.of("keyword", "Redis"))
                .build();
        when(agentPlanner.plan("搜索Redis代码")).thenReturn(List.of(task));

        Skill mockSkill = new StubSkill("code_search", true, Map.of("total", 5));
        when(skillRegistry.findByName("code_search")).thenReturn(Optional.of(mockSkill));

        AgentExecuteResponse response = orchestrator.execute("搜索Redis代码");

        assertEquals("搜索Redis代码", response.getInstruction());
        assertEquals(1, response.getTasks().size());
        assertEquals(1, response.getResults().size());
        assertTrue(response.getResults().get(0).getSuccess());
        assertEquals("code_search", response.getResults().get(0).getSkillName());
        assertTrue(response.getSummary().contains("成功 1"));
        assertTrue(response.getSummary().contains("失败 0"));
    }

    @Test
    @DisplayName("多任务执行全部成功")
    void execute_MultipleTasks_AllSuccess() {
        AgentTask searchTask = AgentTask.builder()
                .taskName("搜索代码")
                .skillName("code_search")
                .params(Map.of("keyword", "Redis"))
                .build();
        AgentTask explainTask = AgentTask.builder()
                .taskName("解释代码")
                .skillName("ai_explain")
                .params(Map.of("question", "解释"))
                .build();

        when(agentPlanner.plan("搜索并解释Redis代码")).thenReturn(List.of(searchTask, explainTask));
        when(skillRegistry.findByName("code_search")).thenReturn(Optional.of(new StubSkill("code_search", true)));
        when(skillRegistry.findByName("ai_explain")).thenReturn(Optional.of(new StubSkill("ai_explain", true)));

        AgentExecuteResponse response = orchestrator.execute("搜索并解释Redis代码");

        assertEquals(2, response.getTasks().size());
        assertEquals(2, response.getResults().size());
        assertTrue(response.getSummary().contains("成功 2"));
        assertTrue(response.getSummary().contains("失败 0"));
    }

    @Test
    @DisplayName("Skill 不存在时记录失败并继续执行")
    void execute_SkillNotFound_RecordsFailure() {
        AgentTask task = AgentTask.builder()
                .taskName("未知任务")
                .skillName("unknown_skill")
                .params(Map.of())
                .build();

        when(agentPlanner.plan("做一个未知任务")).thenReturn(List.of(task));
        when(skillRegistry.findByName("unknown_skill")).thenReturn(Optional.empty());

        AgentExecuteResponse response = orchestrator.execute("做一个未知任务");

        assertEquals(1, response.getResults().size());
        assertFalse(response.getResults().get(0).getSuccess());
        assertEquals("unknown_skill", response.getResults().get(0).getSkillName());
        assertTrue(response.getResults().get(0).getError().contains("不存在"));
        assertTrue(response.getSummary().contains("失败 1"));
    }

    @Test
    @DisplayName("部分成功部分失败时 summary 正确统计")
    void execute_MixedSuccessAndFailure_SummaryCorrect() {
        AgentTask goodTask = AgentTask.builder()
                .taskName("搜索代码")
                .skillName("code_search")
                .params(Map.of("keyword", "test"))
                .build();
        AgentTask badTask = AgentTask.builder()
                .taskName("不存在的Skill")
                .skillName("nonexistent")
                .params(Map.of())
                .build();

        when(agentPlanner.plan("搜索test并做其他事")).thenReturn(List.of(goodTask, badTask));
        when(skillRegistry.findByName("code_search")).thenReturn(Optional.of(new StubSkill("code_search", true)));
        when(skillRegistry.findByName("nonexistent")).thenReturn(Optional.empty());

        AgentExecuteResponse response = orchestrator.execute("搜索test并做其他事");

        assertEquals(2, response.getResults().size());
        assertTrue(response.getResults().get(0).getSuccess());
        assertFalse(response.getResults().get(1).getSuccess());
        assertTrue(response.getSummary().contains("成功 1"));
        assertTrue(response.getSummary().contains("失败 1"));
    }

    @Test
    @DisplayName("上下文在多个 Skill 之间传递")
    void execute_ContextPassedBetweenSkills() {
        AgentTask searchTask = AgentTask.builder()
                .taskName("搜索代码")
                .skillName("code_search")
                .params(Map.of("keyword", "Redis"))
                .build();
        AgentTask explainTask = AgentTask.builder()
                .taskName("解释代码")
                .skillName("ai_explain")
                .params(Map.of())
                .build();

        when(agentPlanner.plan("搜索并解释Redis")).thenReturn(List.of(searchTask, explainTask));

        // 第一个 Skill 会在 context 中设置 search_top_id
        Skill searchSkill = new Skill() {
            @Override public String name() { return "code_search"; }
            @Override public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
                context.put("search_top_id", 42L);
                context.put("search_top_code", "public class Redis {}");
                return SkillResult.builder().success(true).skillName("code_search").data(Map.of("id", 42)).build();
            }
        };
        Skill explainSkill = new Skill() {
            @Override public String name() { return "ai_explain"; }
            @Override public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
                // 验证上下文被传递
                assertEquals(42L, context.get("search_top_id"));
                assertEquals("public class Redis {}", context.get("search_top_code"));
                return SkillResult.builder().success(true).skillName("ai_explain").data(Map.of("answer", "ok")).build();
            }
        };

        when(skillRegistry.findByName("code_search")).thenReturn(Optional.of(searchSkill));
        when(skillRegistry.findByName("ai_explain")).thenReturn(Optional.of(explainSkill));

        AgentExecuteResponse response = orchestrator.execute("搜索并解释Redis");

        assertEquals(2, response.getResults().size());
        assertTrue(response.getResults().get(0).getSuccess());
        assertTrue(response.getResults().get(1).getSuccess());
    }

    @Test
    @DisplayName("规划器返回空任务列表时返回空响应")
    void execute_NoTasks_ReturnsEmptyResponse() {
        when(agentPlanner.plan("hello")).thenReturn(Collections.emptyList());

        AgentExecuteResponse response = orchestrator.execute("hello");

        assertTrue(response.getTasks().isEmpty());
        assertTrue(response.getResults().isEmpty());
        assertTrue(response.getSummary().contains("0 个任务"));
    }

    @Test
    @DisplayName("buildSummary 正确构造包含任务名和 Skill 名的摘要")
    void execute_SummaryContainsTaskDetails() {
        AgentTask task = AgentTask.builder()
                .taskName("搜索代码")
                .skillName("code_search")
                .params(Map.of())
                .build();

        when(agentPlanner.plan("搜索")).thenReturn(List.of(task));
        when(skillRegistry.findByName("code_search")).thenReturn(Optional.of(new StubSkill("code_search", true)));

        AgentExecuteResponse response = orchestrator.execute("搜索");

        assertTrue(response.getSummary().contains("搜索代码"));
        assertTrue(response.getSummary().contains("code_search"));
        assertTrue(response.getSummary().contains("成功"));
    }

    private static final class StubSkill implements Skill {
        private final String name;
        private final Boolean success;
        private final Map<String, Object> data;

        StubSkill(String name, Boolean success) { this(name, success, Map.of()); }
        StubSkill(String name, Boolean success, Map<String, Object> data) {
            this.name = name; this.success = success; this.data = data;
        }

        @Override public String name() { return name; }
        @Override public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
            return SkillResult.builder().success(success).skillName(name).data(data).build();
        }
    }
}
