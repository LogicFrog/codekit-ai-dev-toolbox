# CodeKit 版本管理模块补齐落地手册（回滚 + 完整差异分析 + AI 分析）

本文基于你当前项目真实代码结构编写（Spring Boot + Vue3 + Element Plus），目标是把以下能力一次性补齐并可直接上线使用：

1. 版本回滚（后端接口 + 前端按钮）
2. 更完整的差异分析（后端结构化 diff，不只前端 Monaco 展示）
3. AI 辅助版本分析（输出风险点、建议、测试关注点）

---

## 0. 先确认当前基线（你现在已具备）

你当前已具备：

- 后端创建版本：`POST /api/code/{id}/create-version`
- 后端版本列表：`GET /api/code/{id}/versions`
- 前端版本页：`web/codekit-client/src/views/VersionControl.vue`
- 前端 diff 组件：`web/codekit-client/src/components/DiffEditor.vue`

当前缺失：

- 回滚接口和服务逻辑
- 后端 diff 结构化输出（用于报告/统计/AI）
- AI 分析接口

---

## 1. 设计原则（先统一，避免返工）

### 1.1 回滚设计

- 回滚本质：把 `code_snippet.code_content` 恢复为某个历史版本的 `version_info.code_content`
- 安全策略：回滚前，自动备份“当前内容”成一个新版本（防止误回滚）

### 1.2 差异分析设计

- 前端 Monaco diff 继续保留（视觉对比）
- 新增后端 diff 结果（结构化 JSON）：`added/removed/modified/变更块列表`

### 1.3 AI 分析设计

- 输入：两个版本 + 后端 diff 统计 + 关键代码片段
- 输出：总结、风险、建议、测试关注点
- 复用你现有 `AIService.explain(...)`

---

## 2. 后端改造（一步一步）

## 2.1 新增 DTO

在目录 `src/main/java/org/itfjnu/codekit/code/dto/` 下新增 5 个文件。

### 文件 1：`VersionRollbackResponse.java`

```java
package org.itfjnu.codekit.code.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VersionRollbackResponse {
    private Long snippetId;
    private Long rollbackToVersionId;
    private Long backupVersionId;
    private String backupVersionName;
    private LocalDateTime rollbackTime;
}
```

### 文件 2：`VersionChangeBlock.java`

```java
package org.itfjnu.codekit.code.dto;

import lombok.Data;

@Data
public class VersionChangeBlock {
    // ADD / REMOVE / MODIFY
    private String type;
    private Integer oldStartLine;
    private Integer oldEndLine;
    private Integer newStartLine;
    private Integer newEndLine;
    private String oldSnippet;
    private String newSnippet;
}
```

### 文件 3：`VersionDiffResponse.java`

```java
package org.itfjnu.codekit.code.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VersionDiffResponse {
    private Long snippetId;
    private Long fromVersionId;
    private Long toVersionId;
    private Integer addedLines;
    private Integer removedLines;
    private Integer modifiedBlocks;
    private Double changeRate;
    private String summary;
    private List<VersionChangeBlock> blocks = new ArrayList<>();
}
```

### 文件 4：`VersionAnalyzeRequest.java`

```java
package org.itfjnu.codekit.code.dto;

import lombok.Data;

@Data
public class VersionAnalyzeRequest {
    private Long fromVersionId;
    private Long toVersionId;
    // 可选：比如“只关注性能/安全/可维护性”
    private String focus;
}
```

### 文件 5：`VersionAnalyzeResponse.java`

```java
package org.itfjnu.codekit.code.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VersionAnalyzeResponse {
    private Long snippetId;
    private Long fromVersionId;
    private Long toVersionId;
    private String summary;
    private String riskLevel;
    private List<String> risks = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<String> testFocus = new ArrayList<>();
    private String rawAnswer;
}
```

---

## 2.2 扩展 Repository

修改 `src/main/java/org/itfjnu/codekit/code/repository/VersionInfoRepository.java`：

```java
import java.util.Optional;
```

并增加方法：

```java
Optional<VersionInfo> findByIdAndSnippetId(Long id, Long snippetId);
```

---

## 2.3 扩展 Service 接口

修改 `src/main/java/org/itfjnu/codekit/code/service/VersionInfoService.java`。

新增 import：

```java
import org.itfjnu.codekit.code.dto.VersionAnalyzeRequest;
import org.itfjnu.codekit.code.dto.VersionAnalyzeResponse;
import org.itfjnu.codekit.code.dto.VersionDiffResponse;
import org.itfjnu.codekit.code.dto.VersionRollbackResponse;
```

接口增加：

```java
VersionRollbackResponse rollbackToVersion(Long snippetId, Long versionId);

VersionDiffResponse compareVersions(Long snippetId, Long fromVersionId, Long toVersionId);

VersionAnalyzeResponse analyzeVersions(Long snippetId, VersionAnalyzeRequest request);
```

---

## 2.4 实现 Service（核心）

修改 `src/main/java/org/itfjnu/codekit/code/service/impl/VersionInfoServiceImpl.java`。

### 2.4.1 增加依赖注入

在类字段中补充：

```java
private final org.itfjnu.codekit.code.repository.CodeSnippetRepository codeSnippetRepository;
private final org.itfjnu.codekit.search.service.VectorIndexService vectorIndexService;
private final org.itfjnu.codekit.ai.service.AIService aiService;
```

### 2.4.2 增加 3 个对外方法

把以下方法直接加到实现类中（可编译版本）：

```java
@Override
public org.itfjnu.codekit.code.dto.VersionRollbackResponse rollbackToVersion(Long snippetId, Long versionId) {
    CodeSnippet snippet = getExistingSnippet(snippetId);
    VersionInfo target = getVersionBySnippetOrThrow(snippetId, versionId);

    try {
        // 1) 先备份当前内容
        VersionInfo backup = new VersionInfo();
        backup.setSnippetId(snippetId);
        backup.setVersionName("rollback-backup-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now()));
        backup.setDescription("自动备份：回滚前快照");
        backup.setCodeContent(snippet.getCodeContent());
        backup.setCreateTime(java.time.LocalDateTime.now());
        VersionInfo backupSaved = versionInfoRepository.save(backup);

        // 2) 再回滚
        snippet.setCodeContent(target.getCodeContent());
        snippet.setFileMd5(md5Hex(target.getCodeContent()));
        codeSnippetRepository.save(snippet);
        vectorIndexService.upsertSnippetEmbedding(snippet);

        org.itfjnu.codekit.code.dto.VersionRollbackResponse resp = new org.itfjnu.codekit.code.dto.VersionRollbackResponse();
        resp.setSnippetId(snippetId);
        resp.setRollbackToVersionId(versionId);
        resp.setBackupVersionId(backupSaved.getId());
        resp.setBackupVersionName(backupSaved.getVersionName());
        resp.setRollbackTime(java.time.LocalDateTime.now());
        return resp;
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        throw new ServiceException(ErrorCode.VERSION_RESTORE_FAILED, "版本回滚失败：" + e.getMessage(), e);
    }
}

@Override
public org.itfjnu.codekit.code.dto.VersionDiffResponse compareVersions(Long snippetId, Long fromVersionId, Long toVersionId) {
    getExistingSnippet(snippetId);
    VersionInfo from = getVersionBySnippetOrThrow(snippetId, fromVersionId);
    VersionInfo to = getVersionBySnippetOrThrow(snippetId, toVersionId);

    try {
        return buildDiff(snippetId, from, to);
    } catch (Exception e) {
        throw new ServiceException(ErrorCode.VERSION_COMPARE_FAILED, "版本差异分析失败：" + e.getMessage(), e);
    }
}

@Override
public org.itfjnu.codekit.code.dto.VersionAnalyzeResponse analyzeVersions(Long snippetId, org.itfjnu.codekit.code.dto.VersionAnalyzeRequest request) {
    if (request == null || request.getFromVersionId() == null || request.getToVersionId() == null) {
        throw new BusinessException(ErrorCode.PARAM_INVALID, "fromVersionId/toVersionId 不能为空");
    }

    VersionInfo from = getVersionBySnippetOrThrow(snippetId, request.getFromVersionId());
    VersionInfo to = getVersionBySnippetOrThrow(snippetId, request.getToVersionId());
    org.itfjnu.codekit.code.dto.VersionDiffResponse diff = buildDiff(snippetId, from, to);

    String prompt = buildVersionAnalyzePrompt(diff, request.getFocus());
    String contextCode = "【旧版本】\n" + safeCut(from.getCodeContent(), 6000) + "\n\n【新版本】\n" + safeCut(to.getCodeContent(), 6000);

    org.itfjnu.codekit.ai.dto.AIChatRequest aiReq = new org.itfjnu.codekit.ai.dto.AIChatRequest();
    aiReq.setQuestion(prompt);
    aiReq.setCode(contextCode);
    aiReq.setLanguageType("text");
    org.itfjnu.codekit.ai.dto.AIChatResponse aiResp = aiService.explain(aiReq);

    org.itfjnu.codekit.code.dto.VersionAnalyzeResponse resp = new org.itfjnu.codekit.code.dto.VersionAnalyzeResponse();
    resp.setSnippetId(snippetId);
    resp.setFromVersionId(request.getFromVersionId());
    resp.setToVersionId(request.getToVersionId());
    resp.setSummary(diff.getSummary());
    resp.setRiskLevel(guessRiskLevel(aiResp.getAnswer()));
    resp.setRawAnswer(aiResp.getAnswer());
    if (aiResp.getSuggestions() != null) {
        resp.setSuggestions(aiResp.getSuggestions());
    }
    return resp;
}
```

### 2.4.3 增加私有辅助方法

把下面方法补到 `VersionInfoServiceImpl` 类底部：

```java
private VersionInfo getVersionBySnippetOrThrow(Long snippetId, Long versionId) {
    return versionInfoRepository.findByIdAndSnippetId(versionId, snippetId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "版本不存在，ID: " + versionId));
}

private org.itfjnu.codekit.code.dto.VersionDiffResponse buildDiff(Long snippetId, VersionInfo from, VersionInfo to) {
    String oldText = from.getCodeContent() == null ? "" : from.getCodeContent();
    String newText = to.getCodeContent() == null ? "" : to.getCodeContent();

    String[] oldLines = oldText.split("\\R", -1);
    String[] newLines = newText.split("\\R", -1);

    int min = Math.min(oldLines.length, newLines.length);
    int added = 0;
    int removed = 0;
    int modified = 0;

    java.util.List<org.itfjnu.codekit.code.dto.VersionChangeBlock> blocks = new java.util.ArrayList<>();

    for (int i = 0; i < min; i++) {
        if (!java.util.Objects.equals(oldLines[i], newLines[i])) {
            modified++;
            org.itfjnu.codekit.code.dto.VersionChangeBlock b = new org.itfjnu.codekit.code.dto.VersionChangeBlock();
            b.setType("MODIFY");
            b.setOldStartLine(i + 1);
            b.setOldEndLine(i + 1);
            b.setNewStartLine(i + 1);
            b.setNewEndLine(i + 1);
            b.setOldSnippet(oldLines[i]);
            b.setNewSnippet(newLines[i]);
            blocks.add(b);
        }
    }

    if (newLines.length > oldLines.length) {
        added = newLines.length - oldLines.length;
        org.itfjnu.codekit.code.dto.VersionChangeBlock b = new org.itfjnu.codekit.code.dto.VersionChangeBlock();
        b.setType("ADD");
        b.setOldStartLine(oldLines.length);
        b.setOldEndLine(oldLines.length);
        b.setNewStartLine(oldLines.length + 1);
        b.setNewEndLine(newLines.length);
        b.setOldSnippet("");
        b.setNewSnippet(String.join("\n", java.util.Arrays.copyOfRange(newLines, oldLines.length, newLines.length)));
        blocks.add(b);
    }

    if (oldLines.length > newLines.length) {
        removed = oldLines.length - newLines.length;
        org.itfjnu.codekit.code.dto.VersionChangeBlock b = new org.itfjnu.codekit.code.dto.VersionChangeBlock();
        b.setType("REMOVE");
        b.setOldStartLine(newLines.length + 1);
        b.setOldEndLine(oldLines.length);
        b.setNewStartLine(newLines.length);
        b.setNewEndLine(newLines.length);
        b.setOldSnippet(String.join("\n", java.util.Arrays.copyOfRange(oldLines, newLines.length, oldLines.length)));
        b.setNewSnippet("");
        blocks.add(b);
    }

    int totalBase = Math.max(oldLines.length, 1);
    double rate = ((double) (added + removed + modified)) / totalBase;

    org.itfjnu.codekit.code.dto.VersionDiffResponse resp = new org.itfjnu.codekit.code.dto.VersionDiffResponse();
    resp.setSnippetId(snippetId);
    resp.setFromVersionId(from.getId());
    resp.setToVersionId(to.getId());
    resp.setAddedLines(added);
    resp.setRemovedLines(removed);
    resp.setModifiedBlocks(modified);
    resp.setChangeRate(Math.round(rate * 10000d) / 10000d);
    resp.setSummary(String.format("新增 %d 行，删除 %d 行，修改 %d 处", added, removed, modified));
    resp.setBlocks(blocks);
    return resp;
}

private String buildVersionAnalyzePrompt(org.itfjnu.codekit.code.dto.VersionDiffResponse diff, String focus) {
    String finalFocus = (focus == null || focus.isBlank()) ? "通用质量、潜在风险、测试建议" : focus;
    return "你是资深代码评审工程师。请基于两个版本差异进行分析。\n"
            + "要求：\n"
            + "1) 用中文输出；\n"
            + "2) 先给总体结论；\n"
            + "3) 列出主要风险点；\n"
            + "4) 给出可执行改进建议；\n"
            + "5) 给出测试关注点。\n\n"
            + "差异统计：" + diff.getSummary() + "\n"
            + "关注重点：" + finalFocus;
}

private String guessRiskLevel(String answer) {
    if (answer == null || answer.isBlank()) {
        return "UNKNOWN";
    }
    String text = answer.toLowerCase();
    if (text.contains("高风险") || text.contains("严重") || text.contains("critical")) {
        return "HIGH";
    }
    if (text.contains("中风险") || text.contains("注意") || text.contains("medium")) {
        return "MEDIUM";
    }
    return "LOW";
}

private String safeCut(String text, int max) {
    if (text == null) return "";
    return text.length() <= max ? text : text.substring(0, max);
}

private String md5Hex(String text) {
    try {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] digest = md.digest((text == null ? "" : text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    } catch (Exception e) {
        return null;
    }
}
```

说明：这里用的是“可快速落地”的行级比较算法（足够用于统计和 AI 分析）。后续如果你要做更精细块级 diff，再替换 `buildDiff` 内部算法即可，接口层不需要改。

---

## 2.5 扩展 Controller

修改 `src/main/java/org/itfjnu/codekit/code/controller/CodeSnippetController.java`。

### 2.5.1 新增 import

```java
import org.itfjnu.codekit.code.dto.VersionAnalyzeRequest;
import org.itfjnu.codekit.code.dto.VersionAnalyzeResponse;
import org.itfjnu.codekit.code.dto.VersionDiffResponse;
import org.itfjnu.codekit.code.dto.VersionRollbackResponse;
```

### 2.5.2 增加 3 个接口

```java
@Operation(summary = "版本回滚", description = "将代码片段回滚到指定版本（自动备份当前内容）")
@PostMapping("/{snippetId}/rollback/{versionId}")
public ApiResponse<VersionRollbackResponse> rollbackVersion(@PathVariable Long snippetId,
                                                            @PathVariable Long versionId) {
    return ApiResponse.success(versionInfoService.rollbackToVersion(snippetId, versionId));
}

@Operation(summary = "版本差异分析", description = "对比两个版本并返回结构化差异")
@GetMapping("/{snippetId}/versions/{fromVersionId}/diff/{toVersionId}")
public ApiResponse<VersionDiffResponse> compareVersions(@PathVariable Long snippetId,
                                                        @PathVariable Long fromVersionId,
                                                        @PathVariable Long toVersionId) {
    return ApiResponse.success(versionInfoService.compareVersions(snippetId, fromVersionId, toVersionId));
}

@Operation(summary = "AI 辅助版本分析", description = "调用 AI 对两个版本变更进行风险与建议分析")
@PostMapping("/{snippetId}/versions/analyze")
public ApiResponse<VersionAnalyzeResponse> analyzeVersions(@PathVariable Long snippetId,
                                                           @RequestBody VersionAnalyzeRequest request) {
    return ApiResponse.success(versionInfoService.analyzeVersions(snippetId, request));
}
```

---

## 3. 前端改造（可直接贴代码）

## 3.1 增加类型定义

修改 `web/codekit-client/src/types/index.ts`，在 `VersionInfo` 附近追加：

```ts
export interface VersionChangeBlock {
  type: 'ADD' | 'REMOVE' | 'MODIFY'
  oldStartLine: number
  oldEndLine: number
  newStartLine: number
  newEndLine: number
  oldSnippet: string
  newSnippet: string
}

export interface VersionDiffResponse {
  snippetId: number
  fromVersionId: number
  toVersionId: number
  addedLines: number
  removedLines: number
  modifiedBlocks: number
  changeRate: number
  summary: string
  blocks: VersionChangeBlock[]
}

export interface VersionRollbackResponse {
  snippetId: number
  rollbackToVersionId: number
  backupVersionId: number
  backupVersionName: string
  rollbackTime: string
}

export interface VersionAnalyzeRequest {
  fromVersionId: number
  toVersionId: number
  focus?: string
}

export interface VersionAnalyzeResponse {
  snippetId: number
  fromVersionId: number
  toVersionId: number
  summary: string
  riskLevel: string
  risks: string[]
  suggestions: string[]
  testFocus: string[]
  rawAnswer: string
}
```

---

## 3.2 扩展版本 API

修改 `web/codekit-client/src/api/version.ts`。

新增 import：

```ts
import type {
  VersionInfo,
  CreateVersionRequest,
  VersionDiffResponse,
  VersionRollbackResponse,
  VersionAnalyzeRequest,
  VersionAnalyzeResponse
} from '@/types'
```

新增方法：

```ts
export const rollbackVersion = (snippetId: number, versionId: number): Promise<VersionRollbackResponse> => {
  return request.post<VersionRollbackResponse>(`/code/${snippetId}/rollback/${versionId}`)
}

export const compareVersionsApi = (
  snippetId: number,
  fromVersionId: number,
  toVersionId: number
): Promise<VersionDiffResponse> => {
  return request.get<VersionDiffResponse>(`/code/${snippetId}/versions/${fromVersionId}/diff/${toVersionId}`)
}

export const analyzeVersionsApi = (
  snippetId: number,
  payload: VersionAnalyzeRequest
): Promise<VersionAnalyzeResponse> => {
  return request.post<VersionAnalyzeResponse>(`/code/${snippetId}/versions/analyze`, payload, { timeout: 120000 })
}
```

---

## 3.3 改造 VersionControl 页面

修改 `web/codekit-client/src/views/VersionControl.vue`。

### 3.3.1 在顶部按钮区新增两个按钮

在 `header-right` 区域中，`创建版本` 按钮前后增加：

```vue
<el-button type="warning" @click="handleRollback(versionA!)" v-if="selectedSnippetId && versionA">
  回滚到 A
</el-button>
<el-button type="info" @click="handleAiAnalyze" :loading="analyzing" v-if="versionA && versionB">
  AI 分析
</el-button>
```

### 3.3.2 在 diff 区增加后端统计显示

在 `.diff-stats` 下方增加：

```vue
<div class="server-summary" v-if="serverDiff">
  {{ serverDiff.summary }}，变更率：{{ (serverDiff.changeRate * 100).toFixed(2) }}%
</div>
```

### 3.3.3 增加 AI 分析弹窗

在模板底部追加：

```vue
<el-dialog v-model="showAnalyzeDialog" title="AI 版本分析" width="760px">
  <div v-if="analyzeResult" class="analyze-result">
    <p><strong>风险等级：</strong>{{ analyzeResult.riskLevel }}</p>
    <p><strong>差异摘要：</strong>{{ analyzeResult.summary }}</p>
    <pre class="ai-answer">{{ analyzeResult.rawAnswer }}</pre>
  </div>
  <template #footer>
    <el-button @click="showAnalyzeDialog = false">关闭</el-button>
  </template>
</el-dialog>
```

### 3.3.4 script 中补状态与方法

import 里补充：

```ts
import { listVersions, createVersion, rollbackVersion, compareVersionsApi, analyzeVersionsApi } from '@/api/version'
import type { VersionDiffResponse, VersionAnalyzeResponse } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
```

状态变量：

```ts
const serverDiff = ref<VersionDiffResponse | null>(null)
const showAnalyzeDialog = ref(false)
const analyzeResult = ref<VersionAnalyzeResponse | null>(null)
const analyzing = ref(false)
```

新增方法：

```ts
const loadServerDiff = async () => {
  if (!selectedSnippetId.value || !versionA.value || !versionB.value) {
    serverDiff.value = null
    return
  }
  try {
    serverDiff.value = await compareVersionsApi(selectedSnippetId.value, versionA.value, versionB.value)
  } catch (error) {
    console.error('后端差异分析失败:', error)
    ElMessage.error(extractErrorMessage(error, '后端差异分析失败'))
  }
}

const handleRollback = async (targetVersionId: number) => {
  if (!selectedSnippetId.value) return
  await ElMessageBox.confirm('回滚会覆盖当前代码内容，系统会自动创建备份版本，是否继续？', '确认回滚', { type: 'warning' })
  await rollbackVersion(selectedSnippetId.value, targetVersionId)
  ElMessage.success('回滚成功（已自动备份当前内容）')
  await fetchVersions(selectedSnippetId.value)
}

const handleAiAnalyze = async () => {
  if (!selectedSnippetId.value || !versionA.value || !versionB.value) {
    ElMessage.warning('请先选择两个版本')
    return
  }
  analyzing.value = true
  try {
    analyzeResult.value = await analyzeVersionsApi(selectedSnippetId.value, {
      fromVersionId: versionA.value,
      toVersionId: versionB.value
    })
    showAnalyzeDialog.value = true
  } catch (error) {
    console.error('AI 分析失败:', error)
    ElMessage.error(extractErrorMessage(error, 'AI 分析失败'))
  } finally {
    analyzing.value = false
  }
}
```

监听版本变化时触发后端 diff：

```ts
watch([versionA, versionB], async () => {
  // 你原有的 Monaco diff 统计逻辑保留
  if (versionA.value && versionB.value) {
    setTimeout(() => {
      const diff = diffEditorRef.value?.getDiff?.()
      if (diff) diffResult.value = diff
    }, 500)
    await loadServerDiff()
  } else {
    diffResult.value = null
    serverDiff.value = null
  }
})
```

样式补充（`<style scoped>`）：

```css
.server-summary {
  margin-top: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.ai-answer {
  margin-top: 12px;
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  border-radius: 8px;
  background: #f7f7f8;
  border: 1px solid #e5e7eb;
  white-space: pre-wrap;
  line-height: 1.6;
}
```

---

## 4. 联调验证（命令级）

## 4.1 后端编译

```bash
./mvnw -q -DskipTests compile
```

## 4.2 前端构建

```bash
cd web/codekit-client
npm run build
```

## 4.3 手工接口验证（建议按顺序）

1. 创建版本 A、B（你已有接口）
2. 调 `GET /api/code/{snippetId}/versions/{A}/diff/{B}` 看统计是否返回
3. 调 `POST /api/code/{snippetId}/versions/analyze` 看 AI 文本是否返回
4. 调 `POST /api/code/{snippetId}/rollback/{A}` 看是否成功并自动新增 backup 版本
5. 再查 `GET /api/code/{snippetId}/versions`，确认多了自动备份版本

---

## 5. 验收清单（直接照着勾）

- [ ] 回滚接口成功后，代码内容变更为目标版本内容
- [ ] 回滚后会新增一个 `rollback-backup-*` 版本
- [ ] 后端 diff 返回 `addedLines/removedLines/modifiedBlocks/changeRate`
- [ ] 前端显示“后端差异摘要”
- [ ] 点击“AI 分析”能弹窗展示结果
- [ ] 异常时前端有明确报错（不是静默失败）

---

## 6. 上线前两点增强（强烈建议）

1. 给 `版本回滚` 增加操作人字段和审计日志（后续追责与排障更轻松）
2. 给 AI 分析增加超时降级策略（超时则只展示后端 diff 统计）

---

## 7. 你可以直接复用的提交说明（可选）

```text
feat(version): add rollback, backend diff analysis and AI-assisted version review

- add rollback API with automatic pre-rollback backup snapshot
- add structured version diff API (added/removed/modified/change blocks)
- add AI analyze API for version comparison risk/suggestion output
- enhance VersionControl UI with rollback action and AI analysis dialog
```

