package org.itfjnu.codekit.ai.agent.planner.impl;

import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedAgentPlannerImplTest {

    private final RuleBasedAgentPlannerImpl planner = new RuleBasedAgentPlannerImpl();

    @Test
    @DisplayName("空指令返回兜底 explain 任务")
    void plan_EmptyInstruction_ReturnsFallbackExplain() {
        List<AgentTask> tasks = planner.plan("");

        assertEquals(1, tasks.size());
        assertEquals("ai_explain", tasks.get(0).getSkillName());
        assertEquals("通用问题解释", tasks.get(0).getTaskName());
    }

    @Test
    @DisplayName("null 指令返回兜底 explain 任务")
    void plan_NullInstruction_ReturnsFallbackExplain() {
        List<AgentTask> tasks = planner.plan(null);

        assertEquals(1, tasks.size());
        assertEquals("ai_explain", tasks.get(0).getSkillName());
    }

    @Nested
    @DisplayName("搜索类指令")
    class SearchInstructions {

        @Test
        @DisplayName("\"找\" 关键词触发 code_search")
        void plan_SearchByZhao_TriggersCodeSearch() {
            List<AgentTask> tasks = planner.plan("帮我找一个 Redis 相关的代码");

            assertEquals(1, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("semantic", tasks.get(0).getParams().get("mode"));
        }

        @Test
        @DisplayName("\"搜索\" 关键词触发 code_search")
        void plan_SearchBySousuo_TriggersCodeSearch() {
            List<AgentTask> tasks = planner.plan("搜索数据库连接代码");

            assertEquals(1, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("\"检索\" 关键词触发 code_search")
        void plan_SearchByJiansuo_TriggersCodeSearch() {
            List<AgentTask> tasks = planner.plan("检索所有关于缓存的代码");

            assertEquals(1, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("search 关键词触发 code_search")
        void plan_SearchByEnglish_TriggersCodeSearch() {
            List<AgentTask> tasks = planner.plan("search redis connection code");

            assertEquals(1, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("搜索提取文件名作为关键词")
        void plan_SearchExtractsFileName() {
            List<AgentTask> tasks = planner.plan("帮我找 UserService.java 文件");

            assertEquals(1, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("UserService.java", tasks.get(0).getParams().get("keyword"));
        }
    }

    @Nested
    @DisplayName("语义检索/RAG 指令")
    class RagInstructions {

        @Test
        @DisplayName("\"语义检索\" 关键词触发 rag_retrieve")
        void plan_RagBySemanticSearch_TriggersRagRetrieve() {
            List<AgentTask> tasks = planner.plan("用语义检索查找 Redis 连接池代码");

            assertEquals(1, tasks.size());
            assertEquals("rag_retrieve", tasks.get(0).getSkillName());
            assertEquals(5, tasks.get(0).getParams().get("topK"));
            assertEquals(0.0, tasks.get(0).getParams().get("minScore"));
        }

        @Test
        @DisplayName("rag 关键词触发 rag_retrieve")
        void plan_RagByRagKeyword_TriggersRagRetrieve() {
            List<AgentTask> tasks = planner.plan("使用 RAG 查找认证相关代码");

            assertEquals(1, tasks.size());
            assertEquals("rag_retrieve", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("\"向量检索\" 触发 rag_retrieve")
        void plan_RagByVector_TriggersRagRetrieve() {
            List<AgentTask> tasks = planner.plan("用向量检索找代码");

            assertEquals(1, tasks.size());
            assertEquals("rag_retrieve", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("RAG 优先于普通搜索（语义检索 + 搜索同时出现时走 RAG）")
        void plan_RagTakesPriorityOverSearch() {
            List<AgentTask> tasks = planner.plan("语义检索加搜索缓存代码");

            assertEquals(1, tasks.size());
            assertEquals("rag_retrieve", tasks.get(0).getSkillName());
        }
    }

    @Nested
    @DisplayName("解释类指令")
    class ExplainInstructions {

        @Test
        @DisplayName("\"解释\" 关键词触发 ai_explain")
        void plan_ExplainByJieshi_TriggersAIExplain() {
            List<AgentTask> tasks = planner.plan("帮我解释这段代码的作用");

            assertEquals(1, tasks.size());
            assertEquals("ai_explain", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("\"分析\" 关键词触发 ai_explain")
        void plan_ExplainByFenxi_TriggersAIExplain() {
            List<AgentTask> tasks = planner.plan("分析这段代码的安全性");

            assertEquals(1, tasks.size());
            assertEquals("ai_explain", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("\"风险\" 关键词触发 ai_explain")
        void plan_ExplainByRisk_TriggersAIExplain() {
            List<AgentTask> tasks = planner.plan("评估这段代码的风险");

            assertEquals(1, tasks.size());
            assertEquals("ai_explain", tasks.get(0).getSkillName());
        }

        @Test
        @DisplayName("explain 关键词触发 ai_explain")
        void plan_ExplainByEnglish_TriggersAIExplain() {
            List<AgentTask> tasks = planner.plan("explain this code");

            assertEquals(1, tasks.size());
            assertEquals("ai_explain", tasks.get(0).getSkillName());
        }
    }

    @Nested
    @DisplayName("优化类指令")
    class OptimizeInstructions {

        @Test
        @DisplayName("优化指令同时触发搜索和优化两个任务")
        void plan_Optimize_TriggersSearchAndOptimize() {
            List<AgentTask> tasks = planner.plan("优化这段数据库查询代码的性能");

            assertEquals(2, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("code_optimize", tasks.get(1).getSkillName());
            assertEquals("performance", tasks.get(1).getParams().get("optimizeType"));
        }

        @Test
        @DisplayName("\"重构\" 关键词触发优化")
        void plan_Refactor_TriggersOptimize() {
            List<AgentTask> tasks = planner.plan("重构这段代码");

            assertEquals(2, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("code_optimize", tasks.get(1).getSkillName());
        }

        @Test
        @DisplayName("可读性优化识别")
        void plan_OptimizeReadability() {
            List<AgentTask> tasks = planner.plan("优化这段代码的可读性");

            assertEquals(2, tasks.size());
            assertEquals("code_optimize", tasks.get(1).getSkillName());
            assertEquals("readability", tasks.get(1).getParams().get("optimizeType"));
        }

        @Test
        @DisplayName("Bug 修复优化识别")
        void plan_OptimizeBugfix() {
            List<AgentTask> tasks = planner.plan("优化这段代码并修复 bug");

            assertEquals(2, tasks.size());
            assertEquals("code_optimize", tasks.get(1).getSkillName());
            assertEquals("bugfix", tasks.get(1).getParams().get("optimizeType"));
        }

        @Test
        @DisplayName("直接优化不触发搜索")
        void plan_DirectOptimize_SkipsSearch() {
            List<AgentTask> tasks = planner.plan("直接优化这段代码");

            assertEquals(1, tasks.size());
            assertEquals("code_optimize", tasks.get(0).getSkillName());
        }
    }

    @Nested
    @DisplayName("版本类指令")
    class VersionInstructions {

        @Test
        @DisplayName("版本历史带 snippetId 直接查询")
        void plan_VersionWithSnippetId_QueriesDirectly() {
            List<AgentTask> tasks = planner.plan("查看 snippetId=5 的版本历史");

            assertEquals(1, tasks.size());
            assertEquals("version_list", tasks.get(0).getSkillName());
            assertEquals(5L, tasks.get(0).getParams().get("snippetId"));
        }

        @Test
        @DisplayName("版本历史无 snippetId 先搜索")
        void plan_VersionWithoutSnippetId_SearchesFirst() {
            List<AgentTask> tasks = planner.plan("查看这个文件的版本历史");

            assertEquals(2, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("version_list", tasks.get(1).getSkillName());
        }
    }

    @Nested
    @DisplayName("对比类指令")
    class CompareInstructions {

        @Test
        @DisplayName("对比带 snippetId 直接对比")
        void plan_CompareWithSnippetId_ComparesDirectly() {
            List<AgentTask> tasks = planner.plan("对比 snippetId=3 的版本差异");

            assertEquals(1, tasks.size());
            assertEquals("git_compare", tasks.get(0).getSkillName());
            assertEquals(3L, tasks.get(0).getParams().get("snippetId"));
        }

        @Test
        @DisplayName("对比无 snippetId 先搜索再对比")
        void plan_CompareWithoutSnippetId_SearchesFirst() {
            List<AgentTask> tasks = planner.plan("diff 查看这个代码的版本差异");

            assertEquals(2, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("git_compare", tasks.get(1).getSkillName());
        }
    }

    @Nested
    @DisplayName("复合指令（搜索 + 解释）")
    class CompoundInstructions {

        @Test
        @DisplayName("搜索并解释同时触发两个任务")
        void plan_SearchAndExplain_TriggersBoth() {
            List<AgentTask> tasks = planner.plan("搜索缓存代码并解释风险");

            assertEquals(2, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("ai_explain", tasks.get(1).getSkillName());
        }

        @Test
        @DisplayName("搜索并分析触发搜索和解释")
        void plan_SearchAndAnalyze_TriggersBoth() {
            List<AgentTask> tasks = planner.plan("找 Redis 代码然后分析一下");

            assertEquals(2, tasks.size());
            assertEquals("code_search", tasks.get(0).getSkillName());
            assertEquals("ai_explain", tasks.get(1).getSkillName());
        }
    }

    @Nested
    @DisplayName("关键词提取")
    class KeywordExtraction {

        @Test
        @DisplayName("提取文件名作为关键词")
        void extractKeyword_FileName_Preserved() {
            List<AgentTask> tasks = planner.plan("找 CategoryService.java 的代码");

            assertEquals("CategoryService.java", tasks.get(0).getParams().get("keyword"));
        }

        @Test
        @DisplayName("过滤停用词")
        void extractKeyword_StopWordsAreFiltered() {
            List<AgentTask> tasks = planner.plan("帮我搜索一下代码");

            assertEquals("code_search", tasks.get(0).getSkillName());
            assertNotNull(tasks.get(0).getParams().get("keyword"));
        }

        @Test
        @DisplayName("提取关注点作为关键词")
        void extractKeyword_ExtractsFocus() {
            List<AgentTask> tasks = planner.plan("搜索数据库连接建立代码");

            assertEquals("code_search", tasks.get(0).getSkillName());
            String keyword = (String) tasks.get(0).getParams().get("keyword");
            assertNotNull(keyword);
        }
    }

    @Test
    @DisplayName("snippetId=语法正确提取")
    void extractSnippetId_EqualsSyntax() {
        List<AgentTask> tasks = planner.plan("对比 snippetId=42 的版本");

        assertEquals(1, tasks.size());
        assertEquals(42L, tasks.get(0).getParams().get("snippetId"));
    }

    @Test
    @DisplayName("snippetId: 语法正确提取")
    void extractSnippetId_ColonSyntax() {
        List<AgentTask> tasks = planner.plan("查看 snippetId:100 的历史版本");

        assertEquals(1, tasks.size());
        assertEquals(100L, tasks.get(0).getParams().get("snippetId"));
    }
}
