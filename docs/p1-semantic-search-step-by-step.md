# CodeKit 第一优先级任务：语义检索（RAG）落地（傻瓜式教学）

> 适用对象：你当前这个单用户、本地运行的 CodeKit 项目。  
> 目标：把现在“占位”的 `/api/search/semantic` 变成真正可用的语义检索。

---

## 0. 先说明：为什么它是第一优先级

你的 `report` 里把 **RAG 语义检索** 定义成核心能力，但当前代码中：

1. `SearchController` 写了“语义检索（暂未实现）”  
2. `SearchServiceImpl.semanticSearch()` 直接抛 `FEATURE_NOT_IMPLEMENTED`  
3. 前端 `SearchCenter` 也提示“语义搜索功能开发中”

所以最应该先补的就是它。补完后，项目会从“能关键词搜”升级到“能按语义搜”，价值非常大。

---

## 1. 本次要做到的“可交付结果”

你做完本文档后，应满足：

1. 前端“语义搜索”可点击可返回结果。  
2. 后端 `/api/search/semantic` 不再报未实现。  
3. 新增/更新代码片段后，向量能同步更新。  
4. 删除代码片段后，向量会被删除。  
5. 语义检索结果按相似度排序返回。  

---

## 2. 方案选型（先做最稳的 V1，然后马上切真实）

先做 **RAG Lite V1（本地可跑）**，不先上复杂中间件。

1. 向量存储：MySQL 新表（`code_embedding`）  
2. 向量生成：先用“可替换”的 `EmbeddingService`（先 mock 自检，再立刻接真实 embedding API）  
3. 相似度计算：后端 Java 里做 cosine similarity  
4. 保持接口不变：后续可无痛升级 Milvus Lite

这样做的好处是：你现在就能跑通闭环，不会被外部环境卡住；并且因为接口层提前抽象好，切真实 API 几乎不用改业务层。

---

## 3. 第一步：加数据库表（db-init.sql）

在 `docs/db-init.sql` 增加：

```sql
-- 6. 代码向量表（语义检索）
CREATE TABLE IF NOT EXISTS `code_embedding` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `snippet_id` bigint NOT NULL COMMENT '代码片段ID',
    `embedding_json` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '向量(JSON数组)',
    `embedding_dim` int NOT NULL COMMENT '向量维度',
    `model_name` varchar(100) DEFAULT NULL COMMENT '向量模型名',
    `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snippet_id` (`snippet_id`),
    KEY `idx_update_time` (`update_time`),
    CONSTRAINT `fk_embedding_snippet_id`
        FOREIGN KEY (`snippet_id`) REFERENCES `code_snippet` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码向量表';
```

执行方式：

```bash
mysql -u root -p < /Users/annu/codekit/docs/db-init.sql
```

验收：

```sql
SHOW TABLES LIKE 'code_embedding';
DESC code_embedding;
```

---

## 4. 第二步：新增实体与仓库

### 4.1 新建实体

新建：`src/main/java/org/itfjnu/codekit/search/model/CodeEmbedding.java`

字段建议：

1. `id`  
2. `snippetId`  
3. `embeddingJson`  
4. `embeddingDim`  
5. `modelName`  
6. `createTime` / `updateTime`

### 4.2 新建 Repository

新建：`src/main/java/org/itfjnu/codekit/search/repository/CodeEmbeddingRepository.java`

至少包含：

1. `Optional<CodeEmbedding> findBySnippetId(Long snippetId)`  
2. `void deleteBySnippetId(Long snippetId)`  
3. `List<CodeEmbedding> findAll()`

---

## 5. 第三步：新增 Embedding 服务（可替换设计）

### 5.1 定义接口

新建：`src/main/java/org/itfjnu/codekit/search/service/EmbeddingService.java`

方法：

1. `List<Double> embedText(String text)`  
2. `String modelName()`

### 5.2 先做一个可运行实现（MockEmbeddingService，仅用于快速自检）

新建：`src/main/java/org/itfjnu/codekit/search/service/impl/MockEmbeddingService.java`

做法：

1. 固定输出 128 维向量  
2. 根据字符串 hash 填充，保证同文本向量稳定  
3. 用 `@Primary` 标记成默认实现

说明：这是为了先打通链路。你当前已经拿到真实配置，建议 mock 只保留极短时间，验证完立刻切真实。

### 5.3 立刻切真实 Embedding（方舟）

你给出的配置如下：

```yaml
embedding-api: https://ark.cn-beijing.volces.com/ark/api/v1/embeddings/ep-20260416143913-xtb6f
embedding-model: ep-20260416143913-xtb6f
```

建议放进 `application-local.yml`（示例）：

```yaml
ai:
  ark-api-key: ${ARK_API_KEY:}
  embedding-api: https://ark.cn-beijing.volces.com/ark/api/v1/embeddings/ep-20260416143913-xtb6f
  embedding-model: ep-20260416143913-xtb6f
```

然后新增一个真实实现：  
`src/main/java/org/itfjnu/codekit/search/service/impl/ArkEmbeddingService.java`

核心逻辑（OpenAI 兼容风格）：

1. 请求头：`Authorization: Bearer <ARK_API_KEY>`  
2. 请求体：`model`、`input`  
3. 响应中取 `data[0].embedding` 转成 `List<Double>`

请求体示例：

```json
{
  "model": "ep-20260416143913-xtb6f",
  "input": "请将这段文本转为向量"
}
```

切换规则建议：

1. 默认使用 `ArkEmbeddingService`（`@Primary`）  
2. 保留 `MockEmbeddingService` 作为兜底（`@ConditionalOnProperty`）  
3. 当 `ai.ark-api-key` 为空时，启动失败并明确报错（避免“以为接了真实，实际在跑 mock”）

排错提示（很重要）：

1. 若返回 401：API Key 错。  
2. 若返回 404：`embedding-api` 路径可能不对，优先检查控制台提供的正式 endpoint。  
3. 若返回 400：通常是 `model` 或 `input` 字段格式不符合要求。  

建议你先用 `curl` 验证 endpoint 可用，再接入 Java：

```bash
curl -X POST "https://ark.cn-beijing.volces.com/ark/api/v1/embeddings/ep-20260416143913-xtb6f" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ARK_API_KEY" \
  -d '{"model":"ep-20260416143913-xtb6f","input":"hello codekit"}'
```

若你控制台给的是另一条 embeddings URL，以控制台为准替换这里的地址。

---

## 6. 第四步：新增向量索引服务（核心）

新建：`src/main/java/org/itfjnu/codekit/search/service/VectorIndexService.java`  
实现类：`src/main/java/org/itfjnu/codekit/search/service/impl/VectorIndexServiceImpl.java`

你要实现 4 个方法：

1. `Boolean upsertSnippetEmbedding(CodeSnippet snippet)`  
2. `Boolean deleteSnippetEmbedding(Long snippetId)`  
3. `List<Long> searchTopKByText(String query, int topK)`  
4. `Boolean rebuildAllEmbeddings()`

实现要点：

1. 文本拼接：`fileName + className + packageName + tags + codeContent`  
2. `embedding_json` 用 `ObjectMapper` 存取  
3. 查询时把 query 转向量，逐条算 cosine，相似度降序取 topK

cosine 公式（Java 里实现即可）：

```text
cosine(a,b) = dot(a,b) / (|a| * |b|)
```

---

## 7. 第五步：把语义检索接进 SearchServiceImpl

改文件：`src/main/java/org/itfjnu/codekit/search/service/impl/SearchServiceImpl.java`

改造 `semanticSearch(SearchRequest request)`：

1. 校验关键词不能为空（语义检索必须有 query）  
2. 调 `vectorIndexService.searchTopKByText(keyword, 100)` 拿 snippetId 列表  
3. 用现有 `searchQueryExecutor`/repository 按 ID 查 `CodeSnippet`  
4. 复用 `searchResponseAssembler` 组装 `SearchResponse`  
5. 用 `paginate()` 返回分页结果

注意：

1. 语义检索结果顺序要按相似度，不要被数据库默认排序打乱  
2. 可保留语言和标签过滤（先过滤再算相似，或者算完再过滤，建议先过滤）

---

## 8. 第六步：在代码增删改处同步向量

改文件：`src/main/java/org/itfjnu/codekit/code/service/impl/CodeSnippetServiceImpl.java`

在以下位置调用 `vectorIndexService`：

1. 保存/更新成功后：`upsertSnippetEmbedding(savedSnippet)`  
2. 删除成功后：`deleteSnippetEmbedding(id)`

这样你后续导入新代码，不需要手动重建向量。

---

## 9. 第七步：提供一个“手动重建向量”接口（强烈建议）

可放在 `SearchController` 或单独 `VectorAdminController`：

1. `POST /api/search/semantic/rebuild`  
2. 调用 `vectorIndexService.rebuildAllEmbeddings()`  
3. 返回 `ApiResponse<Boolean>`

用途：

1. 第一次上线时全量建索引  
2. 后期向量模型升级后重建

---

## 10. 第八步：前端 SearchCenter 开关打通

改文件：`web/codekit-client/src/views/SearchCenter.vue`

你要做 3 件事：

1. 去掉“语义搜索开发中”禁用提示  
2. 当用户选“语义搜索”时调用 `/api/search/semantic`  
3. 无结果时给清晰提示：“语义检索未命中，可尝试关键词检索”

---

## 11. 第九步：最小测试清单（必须做）

### 11.1 后端接口测试

1. `POST /api/search/semantic`，`keyword="Redis连接池"`  
2. `POST /api/search/semantic`，`keyword="分页查询"`  
3. `POST /api/search/semantic`，空 keyword（应报参数错误）

### 11.2 数据一致性测试

1. 新增代码片段后，`code_embedding` 新增记录  
2. 修改代码片段后，向量更新时间变化  
3. 删除代码片段后，向量记录消失

### 11.3 前端联调测试

1. 语义搜索能出结果  
2. 分页正常  
3. 切回关键词搜索仍正常（不能被你改坏）

---

## 12. 常见坑（你大概率会遇到）

1. `embedding_json` 反序列化失败  
解决：统一用 `List<Double>`，不要混 `float` 和 `double`。

2. 语义检索非常慢  
解决：先限制候选集（比如先按语言过滤），再做 cosine。

3. 结果排序乱  
解决：在内存按相似度排完后，再按这个顺序组装返回。

4. 导入后搜不到  
解决：检查是否调用了 `upsertSnippetEmbedding`；必要时执行 `rebuild` 接口。

---

## 13. 完成定义（DoD）

满足以下 6 条就算你这项完成：

1. `/api/search/semantic` 可用。  
2. 前端语义检索可操作。  
3. `code_embedding` 数据自动同步。  
4. 至少 3 条语义检索测试通过。  
5. 有重建向量接口。  
6. 文档更新（把本文件和你的测试结果一起提交）。

---

## 14. 下一步（做完本任务后）

完成本任务后再做第二优先级：  
**AI Agent 化（Skill 调度）**，把“检索 -> 分析 -> 输出”从单接口升级为任务链。
