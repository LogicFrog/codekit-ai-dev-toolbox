c# CodeKit 模块复盘文档：AI 聊天 + 语义搜索（面向零基础）

> 目标：你读完这份文档后，能自己回答这 3 个问题：
> 1. 用户点一次按钮，前后端到底发生了什么？
> 2. 哪些类是主干，哪些类是辅助？
> 3. 出问题时我应该先看哪里？

---

## 0. 先建立整体地图（先记住，不用一口气理解）

CodeKit 里这两个模块本质上分别是：

1. **AI 聊天模块**：把用户问题发给大模型，返回回答，并维护多轮上下文。
2. **语义搜索模块**：把“自然语言查询”和“代码片段”都转成向量，再做相似度匹配。

你可以把它们理解为两条主链路：

### 0.1 AI 聊天链路

前端 `AIAssistant.vue`  
→ `web/src/api/ai.ts`  
→ 后端 `AIController`  
→ `AIService`（根据配置选 mock/real）  
→ `RealAIServiceImpl` 调豆包 API  
→ 返回 `AIChatResponse`

同时并行做了会话记忆：

`RealAIServiceImpl`  
→ `SessionHistoryServiceImpl`（内存 + JSON 持久化）

### 0.2 语义搜索链路

前端 `SearchCenter.vue`  
→ `web/src/api/search.ts`  
→ 后端 `SearchController.semanticSearch()`  
→ `SearchServiceImpl.semanticSearch()`  
→ `VectorIndexService.searchTopKByText()`  
→ `EmbeddingService.embedText()`（把文本转向量）  
→ 余弦相似度排序  
→ 查回 `CodeSnippet`  
→ 组装成 `SearchResponse`

---

## 1. 你必须先认识的 12 个核心文件

先只看下面这些，不要一上来全仓库乱读：

1. `src/main/java/org/itfjnu/codekit/ai/controller/AIController.java`
2. `src/main/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImpl.java`
3. `src/main/java/org/itfjnu/codekit/ai/service/impl/SessionHistoryServiceImpl.java`
4. `src/main/java/org/itfjnu/codekit/ai/config/AIConfig.java`
5. `src/main/java/org/itfjnu/codekit/ai/config/AIProperties.java`
6. `src/main/java/org/itfjnu/codekit/search/controller/SearchController.java`
7. `src/main/java/org/itfjnu/codekit/search/service/impl/SearchServiceImpl.java`
8. `src/main/java/org/itfjnu/codekit/search/service/impl/VectorIndexServiceImpl.java`
9. `src/main/java/org/itfjnu/codekit/search/service/impl/HttpEmbeddingServiceImpl.java`
10. `src/main/java/org/itfjnu/codekit/search/service/support/SearchQueryExecutor.java`
11. `web/codekit-client/src/views/AIAssistant.vue`
12. `web/codekit-client/src/views/SearchCenter.vue`

---

## 2. AI 聊天模块：从按钮到回答的完整拆解

## 2.1 前端入口：AIAssistant.vue 做了什么

文件：`web/codekit-client/src/views/AIAssistant.vue`

页面上有两种模式：

1. `chat`（自由对话）
2. `explain`（代码解释）

当你点击“发送”时，`handleSubmit()` 会：

1. 校验输入
2. 组装 `AIChatRequest`
3. chat 模式走 `aiChatStream()`（SSE 流式）
4. explain 模式走 `aiExplain()`（普通 HTTP）
5. 把 `sessionId` 存进 localStorage：`codekit-ai-session-id`

这一步非常关键：

- 你看到“多轮记忆”并不是魔法，核心就是同一个 `sessionId` 连续传给后端。

## 2.2 API 封装层

文件：`web/codekit-client/src/api/ai.ts`

关键函数：

1. `aiChat()`：普通对话（当前页面主流程不走它）
2. `aiChatStream()`：用 `fetch` 手动解析 SSE 数据块
3. `aiExplain()`：代码解释
4. `clearAiSession()`：清会话
5. `getAiSessionMessages()`：拉历史消息
6. `getAiTemperature()` / `setAiTemperature()`：温度读写

SSE 事件类型（后后端对齐）：

1. `chunk`：流式片段
2. `done`：完成
3. `error`：错误

## 2.3 控制器层

文件：`src/main/java/org/itfjnu/codekit/ai/controller/AIController.java`

暴露了 5 个接口：

1. `POST /api/ai/chat`
2. `POST /api/ai/chat/stream`
3. `POST /api/ai/explain`
4. `DELETE /api/ai/session/{sessionId}`
5. `GET /api/ai/session/{sessionId}/messages`

控制器本身逻辑很薄，主要职责是转发给 service。

## 2.4 服务实现层：RealAIServiceImpl

文件：`src/main/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImpl.java`

这是 AI 模块主干。你重点看 3 个方法：

1. `chat()`
2. `chatStream()`
3. `explain()`

### 2.4.1 chat() 实际步骤

1. 校验 `aiProperties.isConfigured()`（没有 API Key 直接报错）
2. 生成/复用 `sessionId`
3. 从 `SessionHistoryService` 取最近 4 轮上下文
4. `buildChatPromptWithHistory()` 拼 prompt
5. `callDoubaoAPI()` 请求模型
6. 把问答都写入会话历史
7. 返回 `AIChatResponse(answer, sessionId)`

### 2.4.2 chatStream() 实际步骤

1. 同样先校验配置
2. 构建 prompt
3. 创建 `SseEmitter`
4. 启动虚拟线程（`Thread.startVirtualThread`）
5. `callDoubaoAPIStream()` 持续读取上游流
6. 按 chunk 推送给前端
7. 全文完成后推 `done` 并写历史

### 2.4.3 explain() 实际步骤

1. 校验配置
2. `buildExplainPrompt()` 组装解释型提示词
3. 请求模型
4. `extractSuggestions()` 从回答里抽取“建议”行（最多 3 条）

## 2.5 多轮记忆：SessionHistoryServiceImpl

文件：`src/main/java/org/itfjnu/codekit/ai/service/impl/SessionHistoryServiceImpl.java`

它做了 3 层保护：

1. **容量保护**：每个会话最多 20 条消息
2. **长度保护**：单条消息最多 8000 字符
3. **过期保护**：30 分钟不活跃会话会清理

存储方式：

1. 内存：`ConcurrentHashMap<String, Deque<ChatMessage>>`
2. 落盘：`data/ai-sessions.json`
3. 定时兜底：每 60 秒 flush 一次（`@Scheduled`）

你可以把它当“轻量会话数据库”。

## 2.6 AI 配置切换

相关文件：

1. `AIProperties.java`
2. `AIConfig.java`
3. `MockAIServiceImpl.java`

规则：

1. `ai.provider=real` 时使用 `RealAIServiceImpl`
2. 否则使用 `MockAIServiceImpl`

这解释了为什么你有时会看到“【Mock模式】”文本。

---

## 3. 语义搜索模块：从查询到结果的完整拆解

## 3.1 前端入口：SearchCenter.vue

文件：`web/codekit-client/src/views/SearchCenter.vue`

核心行为：

1. 用户输入关键词，选择搜索类型（keyword/semantic）
2. `handleSearch()` 根据 `searchType` 决定调哪个 API
3. 展示分页结果
4. 点击结果后再去请求代码详情

注意：前端允许“无关键词，仅语言/标签筛选”，但这是关键词检索侧设计；语义检索后端强制关键词必填。

## 3.2 控制器层

文件：`src/main/java/org/itfjnu/codekit/search/controller/SearchController.java`

核心接口：

1. `POST /api/search/keyword`
2. `POST /api/search/semantic`
3. `POST /api/search/semantic/rebuild`（重建向量索引）
4. `GET /api/search/history`
5. `DELETE /api/search/history`
6. `GET /api/search/hot-keywords`

## 3.3 SearchServiceImpl：两条搜索逻辑

文件：`src/main/java/org/itfjnu/codekit/search/service/impl/SearchServiceImpl.java`

### 3.3.1 keywordSearch()

主要流程：

1. 构建分页参数
2. 参数判空（关键词/语言/标签至少一个）
3. 读 Redis 缓存（key 基于 keyword/language/tag/exact）
4. 缓存 miss 时走 DB 查询
5. `SearchResponseAssembler` 组装结果 + 相关度打分
6. 写回缓存（10 分钟）
7. 保存搜索历史

### 3.3.2 semanticSearch()

主要流程：

1. 关键词为空直接抛 `SEARCH_KEYWORD_EMPTY`
2. `vectorIndexService.searchTopKByText(query, 100)` 拿 ID 排序
3. 按 ID 查 `CodeSnippet`
4. 按语言/标签二次过滤
5. 按向量排序顺序重新排序
6. 组装 `SearchResponse`
7. 手动分页返回

## 3.4 VectorIndexServiceImpl：语义检索核心算法层

文件：`src/main/java/org/itfjnu/codekit/search/service/impl/VectorIndexServiceImpl.java`

### 3.4.1 索引更新

`upsertSnippetEmbedding(CodeSnippet)` 会：

1. 把代码片段多个字段拼成文本：`fileName + className + packageName + tags + codeContent`
2. 调 `EmbeddingService.embedText(text)` 生成向量
3. 存到 `code_embedding` 表（JSON 形式）

### 3.4.2 语义查询

`searchTopKByText(query, topK)` 会：

1. 查询文本转向量
2. 把数据库中所有向量读出来
3. 每条做余弦相似度
4. 降序排序取 topK snippetId

这就是“语义搜索”最核心的数学过程。

## 3.5 向量生成实现：HttpEmbeddingServiceImpl

文件：`src/main/java/org/itfjnu/codekit/search/service/impl/HttpEmbeddingServiceImpl.java`

职责：把文本发给 embedding API，拿回向量。

输入校验：

1. 文本不能为空
2. `ai.embedding-api` 必须配置
3. `ai.embedding-model` 必须配置

解析兼容：

1. OpenAI 格式：`data[0].embedding`
2. DashScope 格式：`output.embeddings[0].embedding`
3. 兜底格式：`embedding`

这让它能兼容多种厂商返回。

## 3.6 数据层模型

关键实体：

1. `CodeSnippet`：代码片段主体
2. `CodeEmbedding`：向量表（`embedding_json`, `embedding_dim`, `model_name`）
3. `SearchHistory`：搜索历史

关键仓库：

1. `CodeSnippetRepository`
2. `CodeEmbeddingRepository`
3. `SearchHistoryRepository`

---

## 4. 前后端响应结构你一定要知道

后端统一返回 `ApiResponse<T>`：

```json
{ "code": 0, "message": "操作成功", "data": ... }
```

前端 `request.ts` 在响应拦截器里自动“剥壳”：

1. 如果有 `code` 和 `data` 字段，就只返回 `data`
2. 所以前端业务代码里通常直接拿到真实数据，不再看到外层 `code/message`

这点很重要，否则你会误以为“前端少了字段”。

---

## 5. 异常和报错是怎么回前端的

核心文件：`GlobalExceptionHandler.java`

处理策略：

1. `BusinessException`：业务异常（如关键词为空）
2. `ServiceException`：服务内部异常（如 AI 未配置）
3. 其他异常统一转标准错误响应

常见你会遇到的错误：

1. `CONFIG_ERROR`：AI 或 embedding 配置缺失
2. `AI_REQUEST_FAILED`：模型请求失败
3. `EMBEDDING_RESULT_EMPTY`：向量响应解析不到结果
4. `SEARCH_KEYWORD_EMPTY`：语义搜索没传关键词

---

## 6. “我该怎么复盘” 的实战步骤（建议照做）

下面是建议你按顺序执行的复盘动作，每一步都能立刻看到结果。

## 6.1 复盘 AI 聊天

1. 在前端进入 AI 助手，选择 chat 模式。
2. 问第一句，不传 sessionId（页面会自动创建）。
3. 问第二句，观察是否能记住第一句上下文。
4. 查看 `data/ai-sessions.json` 是否有会话内容。
5. 点“新对话”，确认旧会话被清理。

你要重点观察的日志点：

1. `RealAIServiceImpl.chat` 开始日志
2. 豆包 API 响应状态码日志
3. 会话历史落盘日志（恢复/写入异常）

## 6.2 复盘语义搜索

1. 先确认数据库有 `code_snippet` 数据。
2. 调 `/api/search/semantic/rebuild` 重建向量。
3. 用自然语言查询 `/api/search/semantic`（例如“连接池超时重试”）。
4. 改用关键词搜索对比结果。
5. 增加语言/标签过滤，观察结果变化。

你要重点观察的日志点：

1. `HttpEmbeddingServiceImpl` 是否成功返回向量
2. `VectorIndexServiceImpl` 是否抛 `SEARCH_FAILED`
3. `SearchServiceImpl.semanticSearch` 分页前后的结果数量

---

## 7. 当前代码里的已知风险（复盘时重点关注）

这部分很关键，是你后续进阶优化的入口。

1. `application.yml` 里没有显式给出 `ai.embedding-api` 和 `ai.embedding-model` 默认值，若本地 profile 未配置，语义检索会报配置错误。
2. `VectorIndexServiceImpl.searchTopKByText()` 当前是“全量向量读内存 + Java 侧逐条计算”，数据量大时会慢。
3. `semanticSearch()` 里 `codeSnippetRepository.findAllById(sortedIds)` 的数据库返回顺序不保证与 `sortedIds` 一致，虽然代码里又按 `idOrder` 做了重排，但这里要牢记“排序正确性靠手动维护”。
4. `SearchHistory.searchType` 在 `keywordSearch()` 中固定写 `0`，语义搜索目前没有记录为 `1`。
5. AI 温度设置在运行期改的是内存值，服务重启后会回到配置文件默认值。
6. 会话历史持久化是 JSON 文件，适合单机/单用户，不适合多实例并发写。

---

## 8. 你可以直接背下来的“主干类口诀”

给你一个记忆法：

1. **入口 Controller**：`AIController` / `SearchController`
2. **主编排 Service**：`RealAIServiceImpl` / `SearchServiceImpl`
3. **核心能力 Service**：`SessionHistoryServiceImpl` / `VectorIndexServiceImpl`
4. **外部适配层**：`HttpEmbeddingServiceImpl`（向量 API）
5. **数据拼装层**：`SearchResponseAssembler`

你每次看不懂业务，就沿这个顺序从上往下走。

---

## 9. 给你的 7 天复盘任务（可选，但很有效）

1. 第 1 天：手抄 AI chat 的一次请求链路（前端函数名 + 后端函数名）。
2. 第 2 天：自己画一张 sessionId 生命周期图。
3. 第 3 天：断点调试 `RealAIServiceImpl.chatStream()`，看 SSE 事件流。
4. 第 4 天：断点调试 `semanticSearch()`，观察 ID 排序如何恢复。
5. 第 5 天：故意删掉 embedding 配置，验证报错路径。
6. 第 6 天：重建索引并记录耗时，理解性能瓶颈。
7. 第 7 天：把“关键词搜索 vs 语义搜索”写成你的个人对照笔记。

---

## 10. 附：接口速查表

### 10.1 AI 模块

1. `POST /api/ai/chat`
2. `POST /api/ai/chat/stream`
3. `POST /api/ai/explain`
4. `DELETE /api/ai/session/{sessionId}`
5. `GET /api/ai/session/{sessionId}/messages?maxRounds=10`
6. `GET /api/ai/settings/temperature`
7. `PUT /api/ai/settings/temperature?value=1.2`

### 10.2 搜索模块

1. `POST /api/search/keyword`
2. `POST /api/search/semantic`
3. `POST /api/search/semantic/rebuild`
4. `GET /api/search/history`
5. `DELETE /api/search/history`
6. `GET /api/search/hot-keywords`

---

## 11. 一句话总结

AI 模块本质是“**会话管理 + 大模型调用**”；语义搜索模块本质是“**文本向量化 + 相似度排序**”。只要你抓住这两条主线，再复杂的代码你都能慢慢拆开。
