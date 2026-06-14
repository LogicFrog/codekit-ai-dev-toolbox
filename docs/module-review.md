# CodeKit 全模块复盘文档

> 生成日期：2026-05-29 | 代码量：Java 9138 行 + Vue/TS 7992 行 | 文件数：129 Java + 30 前端

---

## 项目总览

```
┌──────────────────────────────────────┐
│        Electron 桌面壳                │
│  main.js → spawn JAR → BrowserWindow │
├──────────────────────────────────────┤
│  前端: Vue3 + TS + Pinia + Monaco    │
│  5个 View: 代码管理|搜索|版本|AI|设置  │
├──────────────────────────────────────┤
│  后端: Spring Boot 4 + JDK21         │
│  ┌──────────┬──────────┬──────────┐  │
│  │ AI 模块   │ 代码模块  │ 搜索模块  │  │
│  │ Agent/skill│ 扫描/解析│ 全文/向量│  │
│  │ LLM 服务  │ 版本管理 │ 嵌入服务  │  │
│  └──────────┴──────────┴──────────┘  │
├──────────────────────────────────────┤
│  存储: H2(桌面) / MySQL(开发)         │
└──────────────────────────────────────┘
```

---

## 一、AI Agent 编排引擎

**一句话：** 用户自然语言输入 → LLM 拆解为有序子任务 → 自动调用 Skill 链 → 汇总结果返回。

### 文件结构

```
ai/agent/
├── controller/AIAgentController.java      ← POST /api/ai/agent/execute
├── dto/
│   ├── AgentExecuteRequest.java           ← { instruction, sessionId }
│   ├── AgentExecuteResponse.java          ← { instruction, tasks[], results[], summary }
│   ├── AgentTask.java                     ← { taskName, skillName, params Map }
│   └── SkillResult.java                   ← { success, skillName, data, error }
├── planner/
│   ├── AgentPlanner.java                  ← 接口: List<AgentTask> plan(String)
│   └── impl/
│       ├── LLMAgentPlannerImpl.java       ← @Primary, LLM 动态推理拆解任务
│       └── RuleBasedAgentPlannerImpl.java ← 关键词匹配兜底
├── service/
│   ├── AgentOrchestratorService.java      ← 接口
│   └── impl/AgentOrchestratorServiceImpl.java ← 编排主循环
└── skill/
    ├── Skill.java                         ← 接口: name() + execute(params, context)
    ├── SkillRegistry.java                 ← Spring 自动收集所有 Skill Bean
    └── impl/
        ├── CodeSearchSkillImpl.java       ← skillName="code_search"
        ├── RAGRetrieveSkillImpl.java      ← skillName="rag_retrieve"
        ├── AIExplainSkillImpl.java        ← skillName="ai_explain"
        ├── CodeOptimizeSkillImpl.java     ← skillName="code_optimize"
        ├── GitCompareSkillImpl.java       ← skillName="git_compare"
        └── VersionListSkillImpl.java      ← skillName="version_list"
```

### 工作流程

```
用户输入 "帮我搜索缓存代码并解释风险"
  │
  ▼
AIAgentController → AgentOrchestratorService.execute()
  │
  ├─ (1) AgentPlanner.plan()  ← LLM 推理或规则匹配
  │     └─ 返回: [
  │          AgentTask(taskName="搜索", skillName="code_search", params={keyword:"缓存"}),
  │          AgentTask(taskName="解释", skillName="ai_explain", params={question:"解释风险"})
  │        ]
  │
  ├─ (2) 遍历 tasks:
  │     ├─ SkillRegistry.findByName("code_search") → CodeSearchSkillImpl
  │     ├─ skill.execute(params, sharedContext)    ← 结果写 context{"search_top_id":...}
  │     ├─ SkillRegistry.findByName("ai_explain") → AIExplainSkillImpl
  │     └─ skill.execute(params, sharedContext)    ← 从 context 读 search_top_code
  │
  └─ (3) 返回 AgentExecuteResponse { tasks, results, summary }
```

### 关键设计点

- **Context Map 跨 Skill 数据传递**：`code_search` 把搜到的 id/code/language 写到同一份 `Map<String,Object>`——后续 Skill 直接读。
- **LLM 兜底**：`@Primary` 注解让 LLM 版优先，AI 未配置时 `LLMAgentPlannerImpl.init()` 里自动标记降级。
- **Skill 自动注册**：`SkillRegistry` 用 `List<Skill>` 构造注入，Spring 自动扫描所有 `@Component` 的 Skill 实现。

### 核心文件速查

| 文件 | 行数 | 核心内容 |
|------|------|---------|
| `LLMAgentPlannerImpl.java` | ~120 | 调用 LLM → 解析 JSON → 返回 AgentTask 列表 |
| `RuleBasedAgentPlannerImpl.java` | ~244 | 关键词匹配（找/搜索/解释/优化/对比）→ 组合任务链 |
| `AgentOrchestratorServiceImpl.java` | ~83 | for 循环调用 Skill，Skill 不存在时记录失败继续 |
| `CodeSearchSkillImpl.java` | ~122 | 语义/关键词检索，空结果降级，写 context |
| `AIExplainSkillImpl.java` | ~99 | 多级 code 回退：params → search_top_code → preview → DB |

---

## 二、AI 对话服务

**一句话：** 封装 LLM API 调用，支持 5 家提供商，SSE 流式返回，多轮对话上下文管理。

### 文件结构

```
ai/
├── config/
│   ├── AIProperties.java              ← @ConfigurationProperties("ai")，所有 AI 配置
│   ├── AIConfig.java                  ← @Bean aiService() 根据 provider 选 mock/real
│   ├── LLMProvider.java               ← 枚举：doubao/qwen/openai/deepseek/wenxin
│   └── AIStartupValidator.java        ← 启动校验，缺配置时 warn 不阻断
├── controller/
│   ├── AIController.java              ← POST /ai/chat, /ai/chat/stream, /ai/explain
│   └── AISettingsController.java      ← GET/PUT /ai/settings, /temperature, /providers
├── dto/
│   ├── AIChatRequest.java             ← { question, code, languageType, sessionId }
│   ├── AIChatResponse.java            ← { answer, suggestions, codeBlocks, sessionId }
│   ├── ChatMessage.java               ← { role, content, time, tokenCount }
│   ├── DoubaoRequest.java             ← OpenAI 兼容请求体 { model, messages[], stream, ... }
│   ├── DoubaoResponse.java            ← { choices[].message.content, usage }
│   ├── AISettingsDTO.java             ← 设置 DTO，含 embeddingApiKey
│   └── ProviderInfo.java              ← { code, displayName, defaultBaseUrl, defaultModels }
├── prompt/
│   ├── PromptTemplateType.java        ← 枚举：9 种模板的 resourcePath + 默认文本
│   └── service/
│       ├── PromptTemplateService.java  ← 接口：render/getRawTemplate/reload
│       └── impl/PromptTemplateServiceImpl.java ← 加载 classpath:prompts/*.txt → 缓存 → {key}替换
├── service/
│   ├── AIService.java                 ← 接口：chat/explain/optimize/chatStream
│   ├── AISettingsService.java         ← 接口：温度/设置/提供商
│   └── SessionHistoryService.java     ← 接口：消息追加/获取/清理/token用量
└── impl/
    ├── RealAIServiceImpl.java         ← 真实 LLM 调用（豆包 OpenAI 兼容）
    ├── MockAIServiceImpl.java         ← 模拟返回，开发调试用
    ├── AISettingsServiceImpl.java     ← 设置持久化 + AES 加密保险库
    └── SessionHistoryServiceImpl.java  ← 会话管理：ConcurrentHashMap + JSON 持久化
```

### 关键设计点

- **多提供商切换**：`LLMProvider.fromCode("qwen")` → 自动匹配 baseUrl、模型列表、鉴权方式（都是 OpenAI 兼容格式，只需换 URL 和 Key）
- **AES 加密保险库**：见安全模块
- **会话管理**：4 轮历史、20 条上限、8000 字符截断、30 分钟 TTL、`Scheduled` 定时刷盘

### 核心文件速查

| 文件 | 行数 | 核心内容 |
|------|------|---------|
| `RealAIServiceImpl.java` | ~491 | buildExplainPrompt → callLLMAPI → exchange 读取响应 → parse |
| `AISettingsServiceImpl.java` | ~405 | encrypt/decrypt AES-256-GCM + PBKDF2 + 磁盘 JSON 持久化 |
| `SessionHistoryServiceImpl.java` | ~232 | concurrentHashMap + ArrayDeque + @Scheduled flush + TTL 清理 |
| `PromptTemplateServiceImpl.java` | ~81 | init 加载 9 种模板 → render 做 {key} 替换 |

---

## 三、代码管理（扫描/解析/CRUD）

**一句话：** 扫描本地目录 → 多语言解析 → 结构化存储 → 分类/标签/编辑器联动。

### 文件结构

```
code/
├── controller/
│   ├── CodeSnippetController.java      ← CRUD / 版本 / 分类分配
│   ├── CodeScanController.java         ← POST /scan, GET /scan/status
│   ├── CodeCategoryController.java     ← 分类 CRUD
│   └── GitController.java              ← Git init/commit/status/history/diff
├── model/
│   ├── CodeSnippet.java                ← @Entity: id/filePath/fileName/codeContent/language/...
│   ├── CodeCategory.java               ← @Entity: categoryName/sortOrder
│   ├── CodeDependency.java             ← @Entity: dependName/dependType
│   └── VersionInfo.java                ← @Entity: versionName/codeContent 快照
├── repository/
│   ├── CodeSnippetRepository.java      ← JPA + 全文/LIKE 查询 + 标签查询
│   ├── CodeCategoryRepository.java
│   ├── CodeDependencyRepository.java
│   └── VersionInfoRepository.java
├── service/
│   ├── CodeSnippetService.java         ← 接口
│   ├── VersionInfoService.java         ← 接口：createVersion/list/rollback/compare/analyze
│   ├── CodeCategoryService.java        ← 接口
│   ├── GitService.java                 ← 接口：init/commit/status/diff/isGitRepo
│   └── impl/ (4 个实现)
├── filesystem/                         ← 文件扫描子系统
│   ├── FileScanConstant.java           ← 支持后缀：.java/.py/.js/.vue/.ts；排除目录列表
│   ├── LocalFileScanService.java       ← 虚拟线程异步扫描，Phaser 协调
│   ├── parser/
│   │   ├── CodeMetadataParser.java     ← 接口：parse(file) → CodeParseResult
│   │   ├── CodeMetadataParserResolver.java ← 按语言选择解析器
│   │   ├── CodeParseResult.java        ← { packageName, className, methods[], imports[], deps[] }
│   │   ├── JavaCodeMetadataParser.java ← JavaParser 解析
│   │   ├── PythonCodeMetadataParser.java
│   │   ├── JavaScriptCodeMetadataParser.java
│   │   └── ExternalScriptCodeMetadataParser.java ← 调用外部脚本
│   ├── support/
│   │   ├── CodeFileProcessor.java      ← 读文件 → 解析 → saveOrUpdate
│   │   └── ScanTaskTracker.java        ← ConcurrentHashMap 跟踪扫描状态
│   └── watcher/ProjectFileListener.java ← 文件增删改监听
```

### 关键设计点

- **策略模式解析**：`CodeMetadataParserResolver` 根据文件后缀选择 `JavaCodeMetadataParser` / `PythonCodeMetadataParser` 等
- **JDK21 虚拟线程**：`Executors.newVirtualThreadPerTaskExecutor()` 替代线程池，扫描大量文件时内存占用极低
- **数据库兼容**：`CodeSnippetRepository` 同时支持 MySQL FULLTEXT 和 H2 LIKE（JPQL 查询）

### 核心文件速查

| 文件 | 行数 | 核心内容 |
|------|------|---------|
| `CodeSnippetRepository.java` | ~228 | 17 个 @Query：全文本搜索 + LIKE 搜索 + 标签过滤 |
| `LocalFileScanService.java` | ~130 | 递归扫描 → 虚拟线程异步 → Phaser 等待所有文件处理完 |
| `CodeSnippetServiceImpl.java` | ~170 | saveOrUpdate → 检查 id → 检查路径 → save → upsertEmbedding |
| `VersionInfoServiceImpl.java` | ~300 | buildDiff LCS 算法 + AI 分析版本差异 |

---

## 四、搜索模块（关键词 + RAG）

**一句话：** 关键词全文搜索 + 向量语义搜索双通道，结果缓存 + 热词统计。

### 文件结构

```
search/
├── controller/SearchController.java     ← POST /search/keyword, /search/semantic
├── dto/
│   ├── SearchRequest.java               ← { keyword, searchType, language, tag, exactMatch, page, size }
│   └── SearchResponse.java              ← { id, fileName, codePreview, relevanceScore, ... }
├── model/
│   ├── CodeEmbedding.java               ← @Entity: snippetId/embeddingJson(向量)/embeddingDim
│   └── SearchHistory.java               ← @Entity: keyword/searchType/searchTime
├── repository/
│   ├── CodeEmbeddingRepository.java
│   └── SearchHistoryRepository.java
├── service/
│   ├── SearchService.java               ← 接口：keywordSearch/semanticSearch/history/hotKeywords
│   ├── EmbeddingService.java            ← 接口：embedText(text) → List<Double>
│   └── VectorIndexService.java          ← 接口：upsert/delete/searchTopK
└── impl/
    ├── SearchServiceImpl.java            ← 主逻辑：全文/LIKE检索 + Redis缓存 + 历史记录
    ├── HttpEmbeddingServiceImpl.java     ← 调用阿里云 DashScope / OpenAI Embedding API
    ├── MockEmbeddingServiceImpl.java     ← 模拟向量，开发调试
    ├── VectorIndexServiceImpl.java       ← 向量索引：存/查/余弦相似度计算
    └── support/
        ├── SearchQueryExecutor.java      ← 根据请求类型分发到不同 Repository 方法
        └── SearchResponseAssembler.java  ← List<CodeSnippet> → Page<SearchResponse>
```

### 关键设计点

- **搜索双通道**：`SearchQueryExecutor.loadFullTextSnippets()` → LIKE 查询（H2/MySQL 通用）+ `VectorIndexServiceImpl.searchTopKByText()` → 向量相似度排序
- **结果缓存**：`RedisCacheService` @ConditionalOnBean，有 Redis 就缓存 10min，没有就跳过
- **Embedding API Key 动态读取**：从 `AIProperties.getEmbeddingApiKey()` 实时取，不是 `@Value` 一次性注入

### 核心文件速查

| 文件 | 行数 | 核心内容 |
|------|------|---------|
| `SearchServiceImpl.java` | ~272 | keywordSearch → 查缓存 → 查 DB → 写缓存 → 记历史 |
| `HttpEmbeddingServiceImpl.java` | ~138 | POST DashScope API → Bearer Auth → 解析 embedding |
| `VectorIndexServiceImpl.java` | ~173 | upsert(文本→向量→存DB) + searchTopK(余弦相似度排序) |

---

## 五、安全模块（API Key 加密 + CORS）

**一句话：** API Key 绝不落盘明文，加密密钥与机器绑定。

### 文件结构

```
common/config/CorsConfig.java             ← allowedOriginPattern "*" 跨域
ai/service/impl/AISettingsServiceImpl.java ← 加密保险库核心
```

### 加密流程

```
用户输入 Key → saveSettings
  ↓
AISettingsServiceImpl.saveAllSettings()
  ↓
encrypt(rawKey)  ← PBKDF2 派生 256bit AES Key + SecureRandom IV
  ↓
flushToDiskSafe() → data/ai-settings.json {"encryptedApiKey":"base64..."}
  ↓
下次启动:
  loadFromDisk() → decrypt(encryptedApiKey) → aiProperties.setApiKey(...)
```

### 密码优先级

`CODEKIT_VAULT_PASSWORD` 环境变量 > 本机 hostname 派生

---

## 六、前端（Vue3 + Pinia + Electron）

### 文件结构

```
web/codekit-client/src/
├── App.vue                     ← 主题监听，localStorage 持久化
├── main.ts                     ← createPinia() + ElementPlus + VueRouter
├── router/index.ts             ← createWebHashHistory，5 个路由
├── layouts/MainLayout.vue      ← 可折叠侧边栏导航
├── views/
│   ├── CodeManager.vue         ← 扫描/导入/分类/标签/编辑器
│   ├── SearchCenter.vue        ← 搜索栏/结果列表/代码详情抽屉
│   ├── VersionControl.vue      ← 版本对比 DiffEditor + 回滚
│   ├── AIAssistant.vue         ← Chat/Explain/Agent 三模式 + SSE 流式
│   └── Settings.vue            ← AI配置/编辑器/Embedding Key
├── stores/
│   ├── aiChat.ts               ← mode/loading/conversation/agentResponse + computed
│   ├── settings.ts             ← reactive settings + providers + modelOptions
│   ├── codeManager.ts          ← 代码列表/分类/扫描/导入 + actions
│   ├── search.ts               ← 搜索表单/结果/分页/热门关键词
│   └── versionControl.ts       ← 版本列表/对比/回滚
├── api/
│   ├── ai.ts                   ← aiChat/aiExplain/aiAgentExecute/aiChatStream
│   ├── code.ts                 ← CRUD/scan/versions/assignCategory
│   ├── search.ts               ← keywordSearch/semanticSearch
│   ├── version.ts              ← list/create/compare/analyze/rollback
│   ├── system.ts               ← listFs(文件浏览器)
│   └── git.ts                  ← Git 操作
├── components/
│   ├── CodeEditor.vue          ← Monaco 编辑器封装，fontSize/theme/language props
│   ├── DiffEditor.vue          ← Monaco diff 编辑器
│   ├── FileExplorer.vue        ← 文件浏览器
│   └── AIDrawer.vue            ← AI 侧边面板
├── utils/
│   ├── request.ts              ← axios 实例，Electron 检测切换 baseURL
│   └── helpers.ts              ← debounce/copyToClipboard/formatRelativeTime
└── electron/
    ├── main.js                 ← Electron 主进程：启 JAR → 健康检查 → 创建窗口
    └── preload.js              ← contextBridge 暴露安全 API
```

### 关键设计点

- **Pinia Setup Store**：用 `defineStore('name', () => { ... })` 代替 Options API，5 个 Store 管理全部状态
- **storeToRefs 响应式**：state 属性用 `storeToRefs` 保持响应式，actions 直接解构
- **Electron 适配**：`request.ts` 检测 `userAgent.includes('Electron')` → 切换 baseURL 到 `localhost:18080`
- **Hash 路由**：`createWebHashHistory()` 适配 `file://` 协议

---

## 七、关键技术决策

| 决策 | 为什么 |
|------|--------|
| JDK 21 虚拟线程 | 文件扫描 + AI 响应并发，内存占用极低 |
| H2 嵌入式数据库 | 桌面端零安装依赖 |
| @Lob 替代 LONGTEXT | H2/MySQL 双兼容 |
| JPQL LIKE 替代 FULLTEXT | H2 不支持 MATCH AGAINST |
| AES-256-GCM + PBKDF2 | 银行级加密，密钥不落盘 |
| CUSTOM_DMGBUILD_PATH | 绕过 npmmirror 缺少二进制的问题 |

---

## 八、完整文件索引（129 Java + 30 前端）

### 后端 Java 文件

```
src/main/java/org/itfjnu/codekit/
├── CodekitApplication.java
├── ai/
│   ├── agent/
│   │   ├── controller/AIAgentController.java
│   │   ├── dto/AgentExecuteRequest.java, AgentExecuteResponse.java, AgentTask.java, SkillResult.java
│   │   ├── planner/AgentPlanner.java, impl/LLMAgentPlannerImpl.java, impl/RuleBasedAgentPlannerImpl.java
│   │   ├── service/AgentOrchestratorService.java, impl/AgentOrchestratorServiceImpl.java
│   │   └── skill/Skill.java, SkillRegistry.java, impl/{6个Skill实现}.java
│   ├── config/AIProperties.java, AIConfig.java, LLMProvider.java, AIStartupValidator.java
│   ├── controller/AIController.java, AISettingsController.java
│   ├── dto/AIChatRequest.java, AIChatResponse.java, ChatMessage.java, DoubaoRequest.java, DoubaoResponse.java, AISettingsDTO.java, ProviderInfo.java
│   ├── prompt/PromptTemplateType.java, service/PromptTemplateService.java, service/impl/PromptTemplateServiceImpl.java
│   └── service/AIService.java, AISettingsService.java, SessionHistoryService.java, impl/{4个实现}.java
├── code/
│   ├── controller/CodeSnippetController.java, CodeScanController.java, CodeCategoryController.java, GitController.java
│   ├── dto/{10个DTO}.java
│   ├── filesystem/FileScanConstant.java, LocalFileScanService.java, parser/{7个文件}.java, support/{2个}.java, watcher/{1个}.java
│   ├── model/CodeSnippet.java, CodeCategory.java, CodeDependency.java, VersionInfo.java
│   ├── repository/{4个Repository}.java
│   └── service/{4个接口 + 4个实现}.java
├── common/
│   ├── cache/RedisCacheService.java
│   ├── config/CodeKitProperties.java, CorsConfig.java, FileWatcherConfig.java, JpaConfig.java, OpenApiConfig.java, RedisConfig.java, RestClientConfig.java
│   ├── dto/ApiResponse.java, ErrorCode.java
│   └── exception/BusinessException.java, GlobalExceptionHandler.java, ServiceException.java
├── search/
│   ├── controller/SearchController.java
│   ├── dto/SearchRequest.java, SearchResponse.java
│   ├── model/CodeEmbedding.java, SearchHistory.java
│   ├── repository/CodeEmbeddingRepository.java, SearchHistoryRepository.java
│   ├── service/SearchService.java, EmbeddingService.java, VectorIndexService.java
│   └── impl/SearchServiceImpl.java, HttpEmbeddingServiceImpl.java, MockEmbeddingServiceImpl.java, VectorIndexServiceImpl.java, support/{2个}.java
├── system/controller/FileSystemController.java, dto/FsItem.java
└── utils/TokenEstimator.java
```

### 前端 Vue/TS 文件

```
web/codekit-client/src/
├── App.vue, main.ts
├── router/index.ts
├── layouts/MainLayout.vue
├── views/CodeManager.vue, SearchCenter.vue, VersionControl.vue, AIAssistant.vue, Settings.vue
├── stores/aiChat.ts, settings.ts, codeManager.ts, search.ts, versionControl.ts
├── api/ai.ts, code.ts, search.ts, version.ts, system.ts, git.ts
├── components/CodeEditor.vue, DiffEditor.vue, FileExplorer.vue, AIDrawer.vue
├── utils/request.ts, helpers.ts
├── types/index.ts
└── electron/main.js, preload.js
```
