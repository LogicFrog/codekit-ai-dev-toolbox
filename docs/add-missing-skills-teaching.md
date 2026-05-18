
# 实现缺失的核心 Skill 教学文档

&gt; 目标：这份文档让你照着抄就能完成 CodeOptimizeSkill 和 GitCompareSkill。
&gt; 你按顺序完成后，项目里会新增 2 个可运行的 Skill。

---

## 0. 先确认你当前项目前提

在你当前仓库（`/Users/annu/codekit`）里，以下能力已经存在：

1. AI 解释服务：`AIService.explain()`
2. 版本管理服务：`VersionInfoService`
3. Skill 框架：`Skill` 接口、`SkillRegistry`、`AgentOrchestratorService`
4. 已有 Skill：`code_search`、`ai_explain`、`version_list`

所以我们本次只做 **新增 2 个 Skill**，不重写底层能力。

---

## 1. 一步到位后的效果

你将得到：

1. 新增 Skill：`code_optimize` - 代码优化功能
2. 新增 Skill：`git_compare` - 版本对比功能
3. `RuleBasedAgentPlanner` 支持识别"优化"、"对比"关键词
4. Agent 能调用这两个新 Skill

---

## 2. 本次新增/修改文件总览

请新增/修改以下文件（路径和包名必须一致）：

### 新增文件（2 个）
1. `src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/CodeOptimizeSkillImpl.java`
2. `src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/GitCompareSkillImpl.java`

### 修改文件（1 个）
3. `src/main/java/org/itfjnu/codekit/ai/agent/planner/impl/RuleBasedAgentPlannerImpl.java`

---

## 3. 先给 AIService 加个 optimize 方法（必须第一步）

### 3.1 修改 `AIService.java` 接口

文件路径：`src/main/java/org/itfjnu/codekit/ai/service/AIService.java`

**在第 10 行后面添加一行：**

```java
    AIChatResponse optimize(AIChatRequest request);
```

**完整文件应该是这样：**

```java
package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIService {
    AIChatResponse chat(AIChatRequest request);
    SseEmitter chatStream(AIChatRequest request);
    AIChatResponse explain(AIChatRequest request);
    AIChatResponse optimize(AIChatRequest request); // 新增这行
    String getProviderName();
}
```

---

### 3.2 修改 `RealAIServiceImpl.java` 实现

文件路径：`src/main/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImpl.java`

**先读取这个文件看看结构：**（如果找不到，检查 `MockAIServiceImpl.java`）

**在文件末尾添加这个方法：**

```java
    @Override
    public AIChatResponse optimize(AIChatRequest request) {
        // 优化和解释逻辑一样，只是提示词不同
        // 我们复用 explain 方法，改个问题
        String originalQuestion = request.getQuestion();
        if (originalQuestion == null || originalQuestion.isEmpty()) {
            request.setQuestion("请优化这段代码，提升性能、可读性和安全性，并给出具体的优化建议");
        }
        return explain(request);
    }
```

---

### 3.3 修改 `MockAIServiceImpl.java` 实现

文件路径：`src/main/java/org/itfjnu/codekit/ai/service/impl/MockAIServiceImpl.java`

**同样在文件末尾添加：**

```java
    @Override
    public AIChatResponse optimize(AIChatRequest request) {
        String originalQuestion = request.getQuestion();
        if (originalQuestion == null || originalQuestion.isEmpty()) {
            request.setQuestion("请优化这段代码，提升性能、可读性和安全性，并给出具体的优化建议");
        }
        return explain(request);
    }
```

---

## 4. 写第一个新 Skill：CodeOptimizeSkillImpl（复制即用）

文件路径：`src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/CodeOptimizeSkillImpl.java`

**完整代码，直接复制：**

```java
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
    public SkillResult execute(Map&lt;String, Object&gt; params, Map&lt;String, Object&gt; context) {
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
                        Optional&lt;CodeSnippet&gt; snippet = codeSnippetRepository.findById(snippetId);
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

            // 优化方向：performance/readability/bugfix，默认 all
            String optimizeType = String.valueOf(params.getOrDefault("optimizeType", "all")).trim();
            String question = String.valueOf(params.getOrDefault("question", buildOptimizeQuestion(optimizeType)));

            AIChatRequest req = new AIChatRequest();
            req.setCode(code);
            req.setLanguageType(language);
            req.setQuestion(question);

            AIChatResponse resp = aiService.optimize(req);

            List&lt;String&gt; suggestions = resp.getSuggestions() == null ? List.of() : resp.getSuggestions();
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
            case "performance" -&gt; "请优化这段代码的性能，减少时间复杂度和空间复杂度，给出具体的优化方案和优化后的代码";
            case "readability" -&gt; "请优化这段代码的可读性，重构变量命名、提取方法、简化逻辑，让代码更易理解";
            case "bugfix" -&gt; "请检查这段代码中可能存在的 Bug（空指针、数组越界、逻辑错误等），给出修复方案和修复后的代码";
            default -&gt; "请全面优化这段代码，从性能、可读性、安全性三个方面进行优化，给出具体的优化建议和优化后的代码";
        };
    }
}
```

---

## 5. 写第二个新 Skill：GitCompareSkillImpl（复制即用）

文件路径：`src/main/java/org/itfjnu/codekit/ai/agent/skill/impl/GitCompareSkillImpl.java`

**完整代码，直接复制：**

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
public class GitCompareSkillImpl implements Skill {

    private final VersionInfoService versionInfoService;

    @Override
    public String name() {
        return "git_compare";
    }

    @Override
    public SkillResult execute(Map&lt;String, Object&gt; params, Map&lt;String, Object&gt; context) {
        try {
            // 获取 snippetId，优先从 params 拿，没有从上下文拿
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

            // 获取两个版本 ID
            Object versionAObj = params.get("versionA");
            Object versionBObj = params.get("versionB");

            // 如果没指定版本，用最新两个版本
            List&lt;VersionInfo&gt; allVersions = versionInfoService.listVersions(snippetId);
            if (allVersions.size() &lt; 2) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("版本不足 2 个，无法对比")
                        .build();
            }

            Long versionAId;
            Long versionBId;

            if (versionAObj != null &amp;&amp; versionBObj != null) {
                versionAId = Long.valueOf(String.valueOf(versionAObj));
                versionBId = Long.valueOf(String.valueOf(versionBObj));
            } else {
                // 默认对比最新两个版本
                versionAId = allVersions.get(allVersions.size() - 2).getId();
                versionBId = allVersions.get(allVersions.size() - 1).getId();
            }

            // 获取两个版本的内容
            VersionInfo versionA = versionInfoService.getVersion(versionAId).orElse(null);
            VersionInfo versionB = versionInfoService.getVersion(versionBId).orElse(null);

            if (versionA == null || versionB == null) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("指定的版本不存在")
                        .build();
            }

            // 生成差异分析
            String diffAnalysis = versionInfoService.analyzeVersions(versionAId, versionBId);

            // 写入上下文
            context.put("compare_versionA", versionA);
            context.put("compare_versionB", versionB);
            context.put("compare_analysis", diffAnalysis);

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "snippetId", snippetId,
                            "versionA", versionA,
                            "versionB", versionB,
                            "analysis", diffAnalysis
                    ))
                    .build();

        } catch (Exception e) {
            log.error("GitCompareSkill 执行失败", e);
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

## 6. 修改 RuleBasedAgentPlannerImpl 支持新 Skill

文件路径：`src/main/java/org/itfjnu/codekit/ai/agent/planner/impl/RuleBasedAgentPlannerImpl.java`

**修改 plan() 方法，添加对"优化"、"对比"关键词的识别：**

**在第 38-39 行后面添加：**
```java
        boolean needOptimize = containsAny(lower, "优化", "重构", "improve", "optimize");
        boolean needCompare = containsAny(lower, "对比", "差异", "diff", "compare");
```

**在第 80-89 行（兜底之前）添加：**
```java
        if (needOptimize) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
            String optimizeType = extractOptimizeType(text);
            params.put("optimizeType", optimizeType);
            params.put("question", text);

            tasks.add(AgentTask.builder()
                    .taskName("代码优化")
                    .skillName("code_optimize")
                    .params(params)
                    .build());
        }

        if (needCompare) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
            Long snippetId = extractSnippetId(text);
            if (snippetId != null) {
                params.put("snippetId", snippetId);
            }

            tasks.add(AgentTask.builder()
                    .taskName("版本对比")
                    .skillName("git_compare")
                    .params(params)
                    .build());
        }
```

**在文件末尾添加两个辅助方法：**
```java
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
```

**完整的修改后文件（关键部分）：**
（如果你想省事，直接把整个文件替换成这样）

```java
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
    private static final Set&lt;String&gt; SEARCH_STOP_WORDS = Set.of(
            "帮我", "请", "一下", "帮忙", "搜索", "检索", "查找", "找", "语义", "semantic", "search",
            "代码", "并", "然后", "解释", "分析", "风险", "的", "和", "请问", "查看", "看看", "关于"
    );


    @Override
    public List&lt;AgentTask&gt; plan(String instruction) {
        String text = instruction == null ? "" : instruction.trim();
        String lower = text.toLowerCase();

        List&lt;AgentTask&gt; tasks = new ArrayList&lt;AgentTask&gt;();

        boolean needSearch = containsAny(lower, "找", "搜索", "检索", "search", "语义");
        boolean needExplain = containsAny(lower, "解释", "分析", "风险", "explain");
        boolean needVersion = containsAny(lower, "版本", "历史", "version", "diff");
        boolean needOptimize = containsAny(lower, "优化", "重构", "improve", "optimize");
        boolean needCompare = containsAny(lower, "对比", "差异", "diff", "compare");

        if (needSearch) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
            String keyword = extractSearchKeyword(text);
            params.put("keyword", keyword);
            params.put("fallbackKeyword", text);
            params.put("mode", "semantic");

            tasks.add(AgentTask.builder()
                    .taskName("检索相关代码")
                    .skillName("code_search")
                    .params(params)
                    .build());
        }

        if (needExplain) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
            params.put("question", text);

            tasks.add(AgentTask.builder()
                    .taskName("解释与风险分析")
                    .skillName("ai_explain")
                    .params(params)
                    .build());
        }

        if (needVersion) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
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

        if (needOptimize) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
            String optimizeType = extractOptimizeType(text);
            params.put("optimizeType", optimizeType);
            params.put("question", text);

            tasks.add(AgentTask.builder()
                    .taskName("代码优化")
                    .skillName("code_optimize")
                    .params(params)
                    .build());
        }

        if (needCompare) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
            Long snippetId = extractSnippetId(text);
            if (snippetId != null) {
                params.put("snippetId", snippetId);
            }

            tasks.add(AgentTask.builder()
                    .taskName("版本对比")
                    .skillName("git_compare")
                    .params(params)
                    .build());
        }

        // 兜底
        if (tasks.isEmpty()) {
            Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
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

        LinkedHashSet&lt;String&gt; keywords = Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(token -&gt; !token.isEmpty())
                .filter(token -&gt; !SEARCH_STOP_WORDS.contains(token.toLowerCase()))
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
```

---

## 7. 编译与运行（一步一步）

## 7.1 编译

```bash
cd /Users/annu/codekit
./mvnw -DskipTests compile
```

如果你看到 JDK 错误（如"无效的目标发行版:21"），先把本机 JDK 切到 21。

## 7.2 启动

```bash
./mvnw spring-boot:run
```

## 7.3 Swagger 验证

打开：`http://localhost:8080/swagger-ui/index.html`

找到接口：`POST /api/ai/agent/execute`

---

## 8. 四组可直接复制的测试请求

## 8.1 只做优化（先测这个）

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"帮我优化这段代码，提升性能"}'
```

## 8.2 检索 + 优化（推荐）

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"帮我搜索连接池代码并优化它"}'
```

## 8.3 版本对比（带 snippetId）

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"请对比 snippetId=1 的版本差异"}'
```

## 8.4 检索 + 优化 + 解释（完整版）

```bash
curl -X POST "http://localhost:8080/api/ai/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{"instruction":"帮我搜索 Redis 代码，优化它并解释"}'
```

&gt; 注意：带 snippetId 的请求要求数据库里确实存在对应 id 的代码片段。

---

## 9. 验收标准（你按这个判定"已落地"）

满足以下 5 条，就算你这次真的落地成功：

1. `/api/ai/agent/execute` 可以返回 `200`。
2. 输入"优化"关键词，`tasks` 里能看到 `code_optimize`。
3. 输入"对比"关键词，`tasks` 里能看到 `git_compare`。
4. `code_optimize` 的 `results` 里能看到 `answer` 和 `suggestions`。
5. `git_compare` 的 `results` 里能看到 `versionA` 和 `versionB`。

---

## 10. 常见问题与立刻可用的处理办法

1. **Skill 不存在**：检查 Skill 类上有没有 `@Component` 注解，`name()` 返回的字符串是否和 Planner 里的 `skillName` 完全一致。

2. **AIService.optimize() 找不到**：检查是否忘记修改 `AIService.java` 接口和两个实现类。

3. **git_compare 报版本不足**：先去版本管理页面给某个代码片段创建至少 2 个版本。

4. **code_optimize 代码为空**：确保先做了 `code_search`，或者 params 里传了 code 参数。

---

## 11. 关于 RAGRetrieveSkill 的说明

**你不需要做 RAGRetrieveSkill！** 因为你的 `CodeSearchSkillImpl` 已经支持了语义检索（`mode: "semantic"`），功能完全重复。

技术方案里提到的 RAGRetrieveSkill 其实就是你现在的 CodeSearchSkill，已经实现了！

---

## 12. 你现在做到的能力边界

你完成本文后，已经实现：

1. 5 个完整 Skill：`code_search`、`ai_explain`、`version_list`、`code_optimize`、`git_compare`
2. 完整的 AI Agent 编排链路
3. 规则规划器支持 5 种任务类型
4. Skill 之间上下文传递

但还没实现（可选）：

1. Git 轻量集成（JGit）
2. 插件系统
3. 完整的测试体系

这不影响你"答辩/演示可落地"，因为主链路已经可运行！

