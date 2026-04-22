# AI Agent 化调度（Skill链路）教学文档

> 目标：这份文档不是概念讲解，而是“照着做就能落地”。
> 你按顺序完成后，项目里会新增一个可运行的 Agent 接口：`POST /api/ai/agent/execute`，支持“任务拆解 + Skill 串行编排 + 执行轨迹返回”。

---

## 0. 先确认你当前项目前提

在你当前仓库（`/Users/annu/codekit`）里，以下能力已经存在：

1. AI 聊天/解释服务：`AIService`
2. 检索服务：`SearchService`（关键词/语义）
3. 版本服务：`VersionInfoService`
4. 统一返回：`ApiResponse`

所以我们本次只做 **Agent 编排层**，不重写底层能力。

---

## 1. 一步到位后的效果

你将得到：

1. 新接口：`POST /api/ai/agent/execute`
2. Agent 能把自然语言指令拆成任务（规则 Planner）
3. Agent 能调用 3 个 Skill：
   - `code_search`
   - `ai_explain`
   - `version_list`
4. Skill 按顺序执行，后一个 Skill 能读取前一个 Skill 输出（上下文传递）
5. 返回结构里包含：
   - `tasks`（拆解结果）
   - `results`（每步执行结果）
   - `summary`（最终汇总）

---

## 2. 本次新增文件总览（一次性）

请新增以下 13 个文件（路径和包名必须一致）：

1. `src/main/java/org/itfjnu/codekit/ai/agent/dto/AgentExecuteRequest.java`
2. `src/main/java/org/itfjnu/codekit/ai/agent/dto/AgentExecuteResponse.java`
3. `src/main/java/org/itfjnu/codekit/ai/agent/dto/AgentTask.java`
4. `src/main/java/org/itfjnu/codekit/ai/agent/dto/SkillResult.java`
5. `src/main/java/org/itfjnu/codekit/ai/agent/skill/Skill.java`
6. `src/main/java/org/itfjnu/codekit/ai/agent/skill/SkillRegistry.java`
7. `src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/CodeSearchSkill.java`
8. `src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/AIExplainSkill.java`
9. `src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/VersionListSkill.java`
10. `src/main/java/org/itfjnu/codekit/ai/agent/planner/AgentPlanner.java`
11. `src/main/java/org/itfjnu/codekit/ai/agent/planner/RuleBasedAgentPlanner.java`
12. `src/main/java/org/itfjnu/codekit/ai/agent/service/AgentOrchestratorService.java`
13. `src/main/java/org/itfjnu/codekit/ai/agent/controller/AIAgentController.java`

---

## 3. 先写 DTO（复制即用）

## 3.1 `AgentExecuteRequest.java`

```java
package org.itfjnu.codekit.ai.agent.dto;

import lombok.Data;

@Data
public class AgentExecuteRequest {
    /** 用户自然语言指令 */
    private String instruction;

    /** 可选：会话ID（当前版本先透传，不做会话编排） */
    private String sessionId;
}
```

## 3.2 `AgentTask.java`

```java
package org.itfjnu.codekit.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {
    private String taskName;
    private String skillName;
    private Map<String, Object> params;
}
```

## 3.3 `SkillResult.java`

```java
package org.itfjnu.codekit.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResult {
    private Boolean success;
    private String skillName;
    private Object data;
    private String error;
}
```

## 3.4 `AgentExecuteResponse.java`

```java
package org.itfjnu.codekit.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecuteResponse {
    private String instruction;
    private List<AgentTask> tasks;
    private List<SkillResult> results;
    private String summary;
}
```

---

## 4. 定义 Skill 协议（重点）

## 4.1 `Skill.java`

这里和普通版本不同：我们加了 `context` 参数，用来做 Skill 串联。

```java
package org.itfjnu.codekit.ai.agent.skill;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;

import java.util.Map;

public interface Skill {

    /** Skill 唯一名 */
    String name();

    /**
     * @param params 当前任务参数
     * @param context 整个执行上下文（上一步结果会写到这里）
     */
    SkillResult execute(Map<String, Object> params, Map<String, Object> context);
}
```

## 4.2 `SkillRegistry.java`

```java
package org.itfjnu.codekit.ai.agent.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SkillRegistry {

    private final List<Skill> skills;

    public Optional<Skill> findByName(String name) {
        Map<String, Skill> map = skills.stream()
                .collect(Collectors.toMap(Skill::name, Function.identity(), (a, b) -> a));
        return Optional.ofNullable(map.get(name));
    }
}
```

---

## 5. 写 3 个可落地 Skill

## 5.1 `CodeSearchSkill.java`

```java
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
public class CodeSearchSkill implements Skill {

    private final SearchService searchService;

    @Override
    public String name() {
        return "code_search";
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
                context.put("search_top_preview", items.get(0).getCodePreview());
                context.put("search_top_language", items.get(0).getLanguageType());
                context.put("search_top_id", items.get(0).getId());
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
            log.error("CodeSearchSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }
}
```

## 5.2 `AIExplainSkill.java`

```java
package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIExplainSkill implements Skill {

    private final AIService aiService;

    @Override
    public String name() {
        return "ai_explain";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            // 优先使用 params；如果没传，从上下文拿 code_search 的结果
            String code = String.valueOf(params.getOrDefault("code", "")).trim();
            if (code.isEmpty()) {
                Object fromSearch = context.get("search_top_preview");
                code = fromSearch == null ? "" : String.valueOf(fromSearch);
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
```

## 5.3 `VersionListSkill.java`

```java
package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VersionListSkill implements Skill {

    private final VersionInfoService versionInfoService;

    @Override
    public String name() {
        return "version_list";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            // 优先读 params.snippetId；没有就尝试用 code_search 的 top id
            Object snippetIdObj = params.get("snippetId");
            if (snippetIdObj == null) {
                snippetIdObj = context.get("search_top_id");
            }
            if (snippetIdObj == null) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("缺少 snippetId，且上下文没有 search_top_id")
                        .build();
            }

            Long snippetId = Long.valueOf(String.valueOf(snippetIdObj));
            List<VersionInfo> versions = versionInfoService.listVersions(snippetId);

            context.put("version_count", versions.size());
            context.put("version_items", versions);

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "snippetId", snippetId,
                            "count", versions.size(),
                            "items", versions
                    ))
                    .build();
        } catch (Exception e) {
            log.error("VersionListSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }
}
```

---

## 6. 写任务拆解器（规则版，可直接跑）

## 6.1 `AgentPlanner.java`

```java
package org.itfjnu.codekit.ai.agent.planner;

import org.itfjnu.codekit.ai.agent.dto.AgentTask;

import java.util.List;

public interface AgentPlanner {
    List<AgentTask> plan(String instruction);
}
```

## 6.2 `RuleBasedAgentPlanner.java`

```java
package org.itfjnu.codekit.ai.agent.planner;

import org.itfjnu.codekit.ai.agent.dto.AgentTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedAgentPlanner implements AgentPlanner {

    private static final Pattern SNIPPET_ID_PATTERN = Pattern.compile("snippetId\\s*[=:]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public List<AgentTask> plan(String instruction) {
        String text = instruction == null ? "" : instruction.trim();
        String lower = text.toLowerCase();

        List<AgentTask> tasks = new ArrayList<>();

        boolean needSearch = containsAny(lower, "找", "搜索", "检索", "search", "语义");
        boolean needExplain = containsAny(lower, "解释", "分析", "风险", "explain");
        boolean needVersion = containsAny(lower, "版本", "历史", "version", "diff");

        if (needSearch) {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", text);
            params.put("mode", containsAny(lower, "语义", "semantic") ? "semantic" : "keyword");

            tasks.add(AgentTask.builder()
                    .taskName("检索相关代码")
                    .skillName("code_search")
                    .params(params)
                    .build());
        }

        if (needExplain) {
            Map<String, Object> params = new HashMap<>();
            params.put("question", text);
            // 不传 code，交给 Skill 从上下文拿

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

        // 兜底：什么都没命中时，默认做 explain
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
}
```

---

## 7. 写编排器（Orchestrator）

`AgentOrchestratorService.java`

```java
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
                        .error("Skill 不存在: " + skillName)
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
```

---

## 8. 暴露 Agent 接口

`AIAgentController.java`

```java
package org.itfjnu.codekit.ai.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteRequest;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.itfjnu.codekit.ai.agent.service.AgentOrchestratorService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
@Tag(name = "AI Agent", description = "任务拆解 + Skill 调度")
public class AIAgentController {

    private final AgentOrchestratorService agentOrchestratorService;

    @Operation(summary = "执行 Agent 指令")
    @PostMapping("/execute")
    public ApiResponse<AgentExecuteResponse> execute(@RequestBody AgentExecuteRequest request) {
        AgentExecuteResponse response = agentOrchestratorService.execute(request.getInstruction());
        return ApiResponse.success(response);
    }
}
```

---

## 9. 编译与运行（一步一步）

## 9.1 编译

```bash
./mvnw -DskipTests compile
```

如果你看到 JDK 错误（如“无效的目标发行版:21”），先把本机 JDK 切到 21。

## 9.2 启动

```bash
./mvnw spring-boot:run
```

## 9.3 Swagger 验证

打开：`http://localhost:8080/swagger-ui/index.html`

找到接口：`POST /api/ai/agent/execute`

---

## 10. 三组可直接复制的测试请求

## 10.1 只做检索

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"帮我语义搜索 Redis 连接代码"}'
```

## 10.2 检索 + 解释（推荐先测这个）

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"帮我搜索连接池代码并解释风险"}'
```

## 10.3 版本查询（带 snippetId）

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"请查看 snippetId=1 的版本历史"}'
```

> 注意：第三个请求要求数据库里确实存在 snippetId=1 的代码片段。

---

## 11. 验收标准（你按这个判定“已落地”）

满足以下 5 条，就算你这次真的落地成功：

1. `/api/ai/agent/execute` 可以返回 `200`。
2. 返回中 `tasks` 至少有 1 个任务。
3. 返回中 `results` 的条数和 `tasks` 一致。
4. 返回中 `summary` 能看到“成功/失败统计”。
5. “检索 + 解释”请求能看到 `code_search` 和 `ai_explain` 两步。

---

## 12. 常见问题与立刻可用的处理办法

1. 语义检索返回空：先重建向量索引
```bash
curl -X POST "http://localhost:8080/api/search/semantic/rebuild"
```

2. `ai_explain` 报配置错：检查 `application-local.yml`
- `ai.provider=real`
- `ai.api-key`、`ai.model` 有值

3. `version_list` 报 snippet 不存在：先去页面看真实 snippetId，再改请求里的 `snippetId=xx`。

4. `Skill 不存在`：检查 `RuleBasedAgentPlanner` 里的 `skillName` 是否和 `Skill.name()` 完全一致。

---

## 13. 可选：前端最小接入

你可以在 `web/codekit-client/src/api/ai.ts` 增加一个接口：

```ts
export interface AgentExecuteRequest {
  instruction: string
  sessionId?: string
}

export interface AgentExecuteResponse {
  instruction: string
  tasks: Array<{ taskName: string; skillName: string; params: Record<string, any> }>
  results: Array<{ success: boolean; skillName: string; data?: any; error?: string }>
  summary: string
}

export const aiAgentExecute = (data: AgentExecuteRequest): Promise<AgentExecuteResponse> => {
  return request.post<AgentExecuteResponse>('/ai/agent/execute', data, { timeout: 120000 })
}
```

然后在 AI 页面先加个临时按钮调用它，就可以演示“Agent 链路”。

---

## 14. 你现在做到的能力边界（实话实说）

你完成本文后，已经实现：

1. Agent 编排骨架
2. 多 Skill 调度
3. 任务拆解
4. 上下文串联

但还没实现：

1. LLM 规划器（现在是规则规划器）
2. 并行任务调度
3. 失败重试策略

这不影响你“答辩/演示可落地”，因为主链路已经可运行。
