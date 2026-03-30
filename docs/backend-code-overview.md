# CodeKit 后端代码全面梳理文档

## 写在前面

这份文档会帮你从零开始理解整个 CodeKit 后端项目的代码结构。无论你现在对项目有多陌生，跟着这份文档走一遍，你就能清楚地知道：

- 项目有哪些模块
- 每个模块负责什么
- 代码之间的调用关系
- 数据是如何流动的

---

## 第一部分：项目全景图

### 1.1 项目是什么？

**CodeKit** 是一个"本地代码管理与智能辅助平台"，核心功能：

1. **代码管理**：扫描本地代码文件，存储到数据库
2. **代码检索**：通过关键词搜索代码片段
3. **版本管理**：保存代码的历史版本
4. **AI 辅助**：代码解释、智能对话（目前是壳）

### 1.2 技术栈

| 技术 | 用途 |
|------|------|
| Spring Boot 3.x | 后端框架 |
| Spring Data JPA | 数据库访问 |
| MySQL | 主数据库 |
| Redis | 缓存 |
| Java 21 | 运行环境（虚拟线程） |
| JavaParser | Java 代码解析 |
| Lombok | 简化代码 |

### 1.3 项目目录结构

```
src/main/java/org/itfjnu/codekit/
│
├── CodekitApplication.java        # 启动类（入口）
│
├── common/                        # 公共模块（被所有模块依赖）
│   ├── cache/                     # 缓存服务
│   ├── config/                    # 配置类
│   ├── dto/                       # 公共数据传输对象
│   └── exception/                 # 异常定义
│
├── code/                          # 代码管理模块（核心模块1）
│   ├── controller/                # 控制器（接收请求）
│   ├── dto/                       # 数据传输对象
│   ├── filesystem/                # 文件扫描服务
│   ├── model/                     # 数据库实体
│   ├── repository/                # 数据库访问
│   └── service/                   # 业务逻辑
│
├── search/                        # 检索模块（核心模块2）
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
│
├── ai/                            # AI 模块（核心模块3）
│   ├── config/
│   ├── controller/
│   ├── dto/
│   └── service/
│
└── system/                        # 系统模块
    ├── controller/
    └── dto/
```

---

## 第二部分：公共模块详解

公共模块是所有业务模块的基础设施，理解它对理解整个项目至关重要。

### 2.1 模块结构

```
common/
├── cache/
│   └── RedisCacheService.java      # Redis 缓存服务
├── config/
│   ├── CodeKitProperties.java      # 项目配置属性
│   ├── FileWatcherConfig.java      # 文件监听配置
│   ├── GlobalExceptionHandler.java # 全局异常处理
│   ├── JpaConfig.java              # JPA 配置
│   ├── OpenApiConfig.java          # Swagger 配置
│   └── RedisConfig.java            # Redis 配置
├── dto/
│   ├── ApiResponse.java            # 统一响应格式
│   └── ErrorCode.java              # 错误码枚举
└── exception/
    ├── BusinessException.java      # 业务异常
    └── ServiceException.java       # 服务异常
```

### 2.2 核心类详解

#### 2.2.1 ApiResponse.java - 统一响应格式

**作用**：所有 API 返回的数据都包装成这个格式。

**结构**：
```java
public class ApiResponse<T> {
    private int code;        // 状态码（0=成功，其他=失败）
    private String message;  // 提示信息
    private T data;          // 实际数据
    private long timestamp;  // 时间戳
}
```

**使用示例**：
```java
// 成功响应
return ApiResponse.success(user);

// 失败响应
return ApiResponse.fail(ErrorCode.CODE_NOT_FOUND);
```

**前端收到的 JSON**：
```json
{
    "code": 0,
    "message": "操作成功",
    "data": { ... },
    "timestamp": 1712345678901
}
```

#### 2.2.2 ErrorCode.java - 错误码枚举

**作用**：定义所有可能的错误码，统一管理错误信息。

**错误码规范**：
```
0      : 成功
1xxx   : 通用错误（参数错误、未授权等）
2xxx   : 参数校验错误
3xxx   : 代码管理模块错误
4xxx   : 检索模块错误
5xxx   : AI 模块错误
6xxx   : 版本管理错误
7xxx   : 文件系统错误
9xxx   : 系统错误
```

**常用错误码**：
| 错误码 | 名称 | 含义 |
|--------|------|------|
| 3000 | CODE_NOT_FOUND | 代码片段不存在 |
| 3006 | CODE_SCAN_RUNNING | 扫描任务正在运行 |
| 4000 | SEARCH_KEYWORD_EMPTY | 搜索关键词为空 |
| 5000 | AI_REQUEST_FAILED | AI 请求失败 |
| 7006 | DIRECTORY_NOT_FOUND | 目录不存在 |

#### 2.2.3 BusinessException.java - 业务异常

**作用**：当业务逻辑出现问题时，抛出这个异常。

**使用示例**：
```java
// 简单用法
if (snippet == null) {
    throw new BusinessException(ErrorCode.CODE_NOT_FOUND);
}

// 带自定义消息
throw new BusinessException(ErrorCode.CODE_NOT_FOUND, "文件路径不存在：" + filePath);
```

**异常处理流程**：
```
Controller 抛出 BusinessException
    ↓
GlobalExceptionHandler 捕获
    ↓
转换为 ApiResponse
    ↓
返回给前端
```

#### 2.2.4 GlobalExceptionHandler.java - 全局异常处理器

**作用**：捕获所有异常，统一转换为 ApiResponse 格式。

**处理流程**：
```
任何地方抛出异常
    ↓
GlobalExceptionHandler 捕获
    ↓
根据异常类型选择处理方法
    ↓
返回统一的 ApiResponse
```

**处理的异常类型**：
| 异常类型 | 处理方式 |
|----------|----------|
| BusinessException | 返回业务错误码 |
| ServiceException | 返回服务错误码 |
| MethodArgumentNotValidException | 参数校验失败 |
| DataAccessException | 数据库访问异常 |
| Exception | 未知异常，返回 500 |

#### 2.2.5 CodeKitProperties.java - 项目配置

**作用**：读取 application.yml 中的配置项。

**配置项**：
```yaml
codekit:
  watch:           # 文件监听配置
    enabled: true
    path: /xxx
    interval: 5000
  fs:              # 文件系统配置
    workspace-root: /xxx
```

**使用方式**：
```java
@Autowired
private CodeKitProperties codeKitProperties;

String workspaceRoot = codeKitProperties.getFs().getWorkspaceRoot();
```

#### 2.2.6 RedisCacheService.java - Redis 缓存服务

**作用**：封装 Redis 操作，提供简单的缓存读写能力。

**主要方法**：
```java
// 存入缓存
void set(String key, String value, long timeout, TimeUnit unit);

// 读取缓存
String get(String key);

// 删除缓存
void delete(String key);

// 检查是否存在
Boolean hasKey(String key);
```

---

## 第三部分：代码管理模块详解

这是项目的核心模块之一，负责代码片段的扫描、存储和管理。

### 3.1 模块结构

```
code/
├── controller/
│   └── CodeManagerController.java   # API 接口
├── dto/
│   ├── CreateVersionRequest.java    # 创建版本请求
│   ├── ScanRequest.java             # 扫描请求
│   └── ScanStatusDTO.java           # 扫描状态
├── filesystem/
│   ├── FileScanConstant.java        # 扫描常量
│   ├── LocalFileScanService.java    # 文件扫描服务
│   └── watcher/
│       └── ProjectFileListener.java # 文件监听器
├── model/
│   ├── CodeSnippet.java             # 代码片段实体
│   ├── CodeDependency.java          # 代码依赖实体
│   └── VersionInfo.java             # 版本信息实体
├── repository/
│   ├── CodeSnippetRepository.java   # 代码片段仓库
│   ├── CodeDependencyRepository.java
│   └── VersionInfoRepository.java
└── service/
    ├── CodeManagerService.java      # 服务接口
    └── impl/
        └── CodeManagerServiceImpl.java
```

### 3.2 数据模型详解

#### 3.2.1 CodeSnippet.java - 代码片段实体

**对应数据库表**：`code_snippet`

**字段说明**：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| filePath | String | 文件绝对路径（唯一） |
| fileName | String | 文件名 |
| codeContent | String | 代码内容（LONGTEXT） |
| languageType | String | 语言类型（Java/Python/JS） |
| fileMd5 | String | 文件 MD5（用于去重） |
| packageName | String | 包名 |
| className | String | 类名 |
| tags | Set<String> | 标签集合 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

**关键注解**：
```java
@Entity                           // 标记为 JPA 实体
@Table(name = "code_snippet")     // 对应表名
@ElementCollection                // 标记 tags 为集合
@PrePersist / @PreUpdate          // 自动填充时间
```

#### 3.2.2 CodeDependency.java - 代码依赖实体

**对应数据库表**：`code_dependency`

**字段说明**：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| codeSnippetId | Long | 关联的代码片段 ID |
| dependencyName | String | 依赖名称（如 java.util.List） |

#### 3.2.3 VersionInfo.java - 版本信息实体

**对应数据库表**：`version_info`

**字段说明**：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| snippetId | Long | 关联的代码片段 ID |
| versionName | String | 版本名称 |
| codeContent | String | 该版本的代码内容 |
| description | String | 版本描述 |
| createTime | LocalDateTime | 创建时间 |

### 3.3 服务层详解

#### 3.3.1 CodeManagerService.java - 服务接口

**定义的方法**：
```java
// 保存或更新代码片段
CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet codeSnippet);

// 根据 ID 删除（联动删除依赖）
Boolean deleteCodeSnippetById(Long id);

// 根据 ID 查询
CodeSnippet getCodeSnippetById(Long id);

// 分页查询
Page<CodeSnippet> listCodeSnippetByPage(Pageable pageable);

// 按语言查询
List<CodeSnippet> listCodeSnippetByLanguage(String languageType);

// 按标签查询
List<CodeSnippet> listCodeSnippetByTag(String tag);

// 检查路径是否存在
Boolean isFilePathExists(String filePath);

// 根据路径获取
CodeSnippet getCodeSnippetByPath(String filePath);

// 根据 MD5 获取
CodeSnippet getCodeSnippetByMd5(String fileMd5);

// 根据路径删除
Boolean deleteByFilePath(String filePath);

// 获取所有文件路径
List<String> getAllFilePaths();

// 查询依赖
List<CodeDependency> listDependenciesBySnippetId(Long snippetId);

// 保存依赖
Boolean saveDependencies(Long snippetId, List<String> importList);
```

#### 3.3.2 CodeManagerServiceImpl.java - 服务实现

**核心逻辑**：

**1. 保存或更新代码片段**：
```
接收 CodeSnippet
    ↓
判断是否有 ID
    ├── 有 ID → 查询现有记录 → 更新字段
    └── 无 ID → 检查路径是否存在
                 ├── 路径存在 → 更新
                 └── 路径不存在 → 新增
    ↓
保存到数据库
```

**2. 删除代码片段**：
```
接收 ID
    ↓
查询是否存在
    ├── 不存在 → 抛出异常
    └── 存在 → 删除依赖 → 删除版本 → 删除代码片段
    ↓
返回成功
```

### 3.4 文件扫描服务详解

#### 3.4.1 LocalFileScanService.java

**这是项目中最复杂的服务之一**，负责扫描本地代码目录。

**核心功能**：
1. 递归扫描目录
2. 解析代码文件
3. 提取元数据（类名、方法、依赖）
4. 去重处理
5. 异步执行

**关键变量**：
```java
// 虚拟线程执行器（Java 21 特性）
ExecutorService scanExecutor = Executors.newVirtualThreadPerTaskExecutor();

// 扫描状态缓存
Map<String, String> scanStatusMap;  // IDLE / RUNNING / COMPLETED / FAILED

// 扫描进度缓存
Map<String, AtomicInteger> scanProgressMap;

// 统计计数
Map<String, AtomicInteger> scanSuccessCountMap;
Map<String, AtomicInteger> scanSkipCountMap;
Map<String, AtomicInteger> scanFailedCountMap;
```

**扫描流程**：
```
scanLocalCodeDir(scanDir)
    ↓
校验目录是否存在
    ↓
检查是否正在扫描
    ↓
启动异步任务
    ↓
┌─────────────────────────────────────┐
│  清理僵尸数据                        │
│      ↓                              │
│  递归扫描目录                        │
│      ↓                              │
│  对每个文件：                        │
│    ├── 检查文件大小                  │
│    ├── 计算 MD5                      │
│    ├── 检查是否已存在                │
│    │   ├── 路径存在 + MD5 相同 → 跳过 │
│    │   ├── 路径存在 + MD5 不同 → 更新 │
│    │   ├── MD5 存在 + 路径不同 → 移动 │
│    │   └── 都不存在 → 新增           │
│    ├── 解析代码（JavaParser）        │
│    ├── 提取元数据                    │
│    └── 保存到数据库                  │
│      ↓                              │
│  更新扫描状态                        │
└─────────────────────────────────────┘
```

**processFile 方法详解**：
```java
private CodeSnippet processFile(File codeFile, String rootDir, 
                                 String languageType, String tag) {
    // 1. 检查文件大小（防止 OOM）
    if (codeFile.length() > MAX_FILE_SIZE) {
        return null;
    }
    
    // 2. 计算 MD5
    String fileMd5 = calculateFileMd5(codeFile);
    
    // 3. 检查数据库
    CodeSnippet existingByPath = codeManagerService.getCodeSnippetByPath(filePath);
    CodeSnippet existingByMd5 = codeManagerService.getCodeSnippetByMd5(fileMd5);
    
    // 4. 处理不同情况
    if (existingByPath != null) {
        if (fileMd5.equals(existingByPath.getFileMd5())) {
            // 内容没变，跳过
            return existingByPath;
        } else {
            // 内容变了，更新
            existingByPath.setCodeContent(newContent);
            existingByPath.setFileMd5(fileMd5);
            return codeManagerService.saveOrUpdateCodeSnippet(existingByPath);
        }
    }
    
    if (existingByMd5 != null) {
        // 文件被移动了，更新路径
        existingByMd5.setFilePath(filePath);
        return codeManagerService.saveOrUpdateCodeSnippet(existingByMd5);
    }
    
    // 5. 全新文件
    CodeSnippet snippet = new CodeSnippet();
    snippet.setFilePath(filePath);
    snippet.setCodeContent(codeContent);
    // ... 设置其他字段
    
    // 6. 解析代码
    deepAnalyzeAndParse(snippet, importList);
    
    // 7. 保存
    return codeManagerService.saveOrUpdateCodeSnippet(snippet);
}
```

### 3.5 控制器详解

#### 3.5.1 CodeManagerController.java

**API 接口列表**：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/code/scan | 扫描目录 |
| GET | /api/code/scan/status | 查询扫描状态 |
| POST | /api/code/save-by-path | 按路径导入文件 |
| POST | /api/code/save | 保存代码片段 |
| DELETE | /api/code/delete/{id} | 删除代码片段 |
| GET | /api/code/get/{id} | 查询代码片段 |
| GET | /api/code/tag | 按标签查询 |
| GET | /api/code/language | 按语言查询 |
| GET | /api/code/page | 分页查询 |
| GET | /api/code/{id}/dependencies | 查询依赖 |
| POST | /api/code/{id}/create-version | 创建版本 |
| GET | /api/code/{id}/versions | 查询版本列表 |

### 3.6 数据仓库详解

#### 3.6.1 CodeSnippetRepository.java

**主要查询方法**：

```java
// 基础查询
Optional<CodeSnippet> findByFilePath(String filePath);
CodeSnippet findByFileMd5(String fileMd5);
List<CodeSnippet> findByLanguageType(String languageType);

// 全文搜索（MySQL FULLTEXT）
@Query(value = "SELECT * FROM code_snippet WHERE MATCH(...) AGAINST(:keyword)", ...)
List<CodeSnippet> fullTextSearch(@Param("keyword") String keyword);

// 精确匹配（LIKE）
List<CodeSnippet> findByCodeContentContaining(String keyword);
List<CodeSnippet> findByFileNameContaining(String fileName);

// 组合查询
List<CodeSnippet> findByLanguageTypeAndTagName(String languageType, String tag);
```

---

## 第四部分：检索模块详解

### 4.1 模块结构

```
search/
├── controller/
│   └── SearchController.java    # API 接口
├── dto/
│   ├── SearchRequest.java       # 搜索请求
│   └── SearchResponse.java      # 搜索响应
├── model/
│   └── SearchHistory.java       # 搜索历史实体
├── repository/
│   └── SearchHistoryRepository.java
└── service/
    ├── SearchService.java       # 服务接口
    └── impl/
        └── SearchServiceImpl.java
```

### 4.2 数据模型

#### 4.2.1 SearchRequest.java - 搜索请求

```java
public class SearchRequest {
    private String keyword;        // 搜索关键词
    private String languageType;   // 语言类型过滤
    private String tag;            // 标签过滤
    private Boolean exactMatch;    // 是否精确匹配
    private Integer page = 0;      // 页码
    private Integer size = 10;     // 每页大小
}
```

#### 4.2.2 SearchResponse.java - 搜索响应

```java
public class SearchResponse {
    private Long id;
    private String filePath;
    private String fileName;
    private String codePreview;    // 代码预览（截取前几行）
    private String languageType;
    private String className;
    private Double relevanceScore; // 相关性得分
}
```

#### 4.2.3 SearchHistory.java - 搜索历史

**对应数据库表**：`search_history`

```java
public class SearchHistory {
    private Long id;
    private String keyword;        // 搜索关键词
    private String userId;         // 用户 ID
    private LocalDateTime searchTime; // 搜索时间
}
```

### 4.3 服务层详解

#### 4.3.1 SearchServiceImpl.java

**核心方法：keywordSearch**

```
接收 SearchRequest
    ↓
检查参数（至少要有 keyword、languageType 或 tag 之一）
    ↓
构建缓存 Key
    ↓
检查 Redis 缓存
    ├── 命中 → 直接返回缓存结果
    └── 未命中 → 查询数据库
        ↓
    根据参数选择查询方式
        ├── 有关键词 → 全文搜索
        ├── 只有语言 → 按语言查询
        ├── 只有标签 → 按标签查询
        └── 组合条件 → 组合查询
        ↓
    转换为 SearchResponse
        ↓
    计算相关性得分
        ↓
    分页处理
        ↓
    存入缓存
        ↓
    保存搜索历史
    ↓
返回结果
```

**缓存 Key 格式**：
```
search:keyword:{keyword}:language:{language}:tag:{tag}:exact:{exact}
```

### 4.4 控制器详解

#### 4.4.1 SearchController.java

**API 接口列表**：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/search/keyword | 关键词检索 |
| POST | /api/search/semantic | 语义检索（未实现） |
| GET | /api/search/history | 获取搜索历史 |
| DELETE | /api/search/history | 清空搜索历史 |
| GET | /api/search/hot-keywords | 获取热门关键词 |

---

## 第五部分：AI 模块详解

### 5.1 模块结构

```
ai/
├── config/
│   └── AIConfig.java           # AI 配置
├── controller/
│   └── AIController.java       # API 接口
├── dto/
│   ├── AIChatRequest.java      # 聊天请求
│   └── AIChatResponse.java     # 聊天响应
└── service/
    ├── AIService.java          # 服务接口
    └── impl/
        ├── MockAIServiceImpl.java  # 模拟实现
        └── RealAIServiceImpl.java  # 真实实现（待完善）
```

### 5.2 数据模型

#### 5.2.1 AIChatRequest.java

```java
public class AIChatRequest {
    private String question;      // 问题
    private String code;          // 代码内容
    private String languageType;  // 语言类型
    private String sessionId;     // 会话 ID
}
```

#### 5.2.2 AIChatResponse.java

```java
public class AIChatResponse {
    private String answer;              // 回答
    private List<String> suggestions;   // 建议
    private List<CodeBlock> codeBlocks; // 代码块
    private String error;               // 错误信息
    
    public static class CodeBlock {
        private String language;
        private String code;
        private String description;
    }
}
```

### 5.3 服务层

#### 5.3.1 AIService.java - 服务接口

```java
public interface AIService {
    AIChatResponse chat(AIChatRequest request);    // 通用对话
    AIChatResponse explain(AIChatRequest request); // 代码解释
    String getProviderName();                      // 获取提供者名称
}
```

#### 5.3.2 MockAIServiceImpl.java - 模拟实现

**作用**：在 AI 未配置时返回模拟数据。

**实现逻辑**：
```java
public AIChatResponse chat(AIChatRequest request) {
    // 返回模拟的回答
    return AIChatResponse.builder()
            .answer("这是模拟回答...")
            .suggestions(List.of("建议1", "建议2"))
            .build();
}
```

#### 5.3.3 RealAIServiceImpl.java - 真实实现

**当前状态**：返回错误提示，等待接入真实 AI。

### 5.4 控制器

#### 5.4.1 AIController.java

**API 接口列表**：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/ai/chat | AI 对话 |
| POST | /api/ai/explain | 代码解释 |

---

## 第六部分：系统模块详解

### 6.1 模块结构

```
system/
├── controller/
│   └── FileSystemController.java  # 文件系统接口
└── dto/
    └── FsItem.java                # 文件系统项
```

### 6.2 控制器详解

#### 6.2.1 FileSystemController.java

**功能**：提供文件浏览器功能，让前端可以浏览工作区目录。

**安全机制**：
1. 只允许访问配置的工作区根目录
2. 路径规范化，防止目录穿越攻击
3. 隐藏文件不显示

**API 接口**：

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | /api/fs/list | 列出目录内容 |

**请求示例**：
```
GET /api/fs/list           → 列出工作区根目录
GET /api/fs/list?path=/xxx → 列出指定目录
```

**响应示例**：
```json
{
    "code": 0,
    "data": [
        {"name": "src", "path": "/xxx/src", "isDirectory": true},
        {"name": "pom.xml", "path": "/xxx/pom.xml", "isDirectory": false}
    ]
}
```

---

## 第七部分：数据流图解

### 7.1 代码扫描流程

```
前端发起扫描请求
    ↓
POST /api/code/scan
    ↓
CodeManagerController.scanLocalCode()
    ↓
LocalFileScanService.scanLocalCodeDir()
    ↓
┌─────────────────────────────────────┐
│  异步任务启动                        │
│      ↓                              │
│  扫描状态 = RUNNING                  │
│      ↓                              │
│  递归遍历目录                        │
│      ↓                              │
│  对每个文件：                        │
│    processFile()                    │
│      ↓                              │
│    CodeManagerService               │
│      ↓                              │
│    CodeSnippetRepository            │
│      ↓                              │
│    MySQL 数据库                      │
│      ↓                              │
│  扫描状态 = COMPLETED                │
└─────────────────────────────────────┘
    ↓
返回 true（任务已启动）
```

### 7.2 代码搜索流程

```
前端发起搜索请求
    ↓
POST /api/search/keyword
    ↓
SearchController.keywordSearch()
    ↓
SearchServiceImpl.keywordSearch()
    ↓
检查 Redis 缓存
    ├── 命中 → 返回缓存结果
    └── 未命中 ↓
        CodeSnippetRepository.fullTextSearch()
            ↓
        MySQL FULLTEXT 搜索
            ↓
        转换为 SearchResponse
            ↓
        存入 Redis 缓存
            ↓
        保存搜索历史
    ↓
返回 Page<SearchResponse>
```

### 7.3 AI 对话流程

```
前端发起对话请求
    ↓
POST /api/ai/chat
    ↓
AIController.chat()
    ↓
AIService.chat()
    ├── provider=mock → MockAIServiceImpl
    └── provider=real → RealAIServiceImpl
    ↓
返回 AIChatResponse
```

---

## 第八部分：关键设计模式

### 8.1 分层架构

```
Controller 层（接收请求）
    ↓
Service 层（业务逻辑）
    ↓
Repository 层（数据访问）
    ↓
数据库
```

**好处**：
- 职责清晰
- 易于测试
- 易于维护

### 8.2 接口与实现分离

```java
// 接口
public interface CodeManagerService {
    CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet snippet);
}

// 实现
@Service
public class CodeManagerServiceImpl implements CodeManagerService {
    @Override
    public CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet snippet) {
        // 具体实现
    }
}
```

**好处**：
- 可以有多个实现（如 Mock 和 Real）
- 易于切换实现
- 易于单元测试

### 8.3 统一响应格式

所有 API 都返回 `ApiResponse<T>`：

```java
// 成功
return ApiResponse.success(data);

// 失败
return ApiResponse.fail(ErrorCode.CODE_NOT_FOUND);
```

**好处**：
- 前端处理逻辑统一
- 错误处理统一
- 日志记录统一

### 8.4 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessException ex) {
        // 统一处理
    }
}
```

**好处**：
- 不需要在每个方法里 try-catch
- 错误响应格式统一
- 日志记录统一

---

## 第九部分：配置文件详解

### 9.1 application.yml 结构

```yaml
spring:
  application:
    name: codekit
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}  # 激活的配置文件
  
  datasource:                  # 数据库配置
    url: ${CODEKIT_DB_URL:...}
    username: ${CODEKIT_DB_USERNAME:}
    password: ${CODEKIT_DB_PASSWORD:}
  
  data:
    redis:                     # Redis 配置
      host: ${CODEKIT_REDIS_HOST:localhost}
      port: ${CODEKIT_REDIS_PORT:6379}

codekit:                       # 项目自定义配置
  watch:
    enabled: true
    path: ${CODEKIT_WATCH_PATH:}
  fs:
    workspace-root: ${CODEKIT_WORKSPACE_ROOT:}

ai:                            # AI 配置
  provider: ${CODEKIT_AI_PROVIDER:mock}
```

### 9.2 application-local.yml

**用途**：存放本地开发环境的敏感配置。

```yaml
spring:
  datasource:
    username: root
    password: your_password

codekit:
  fs:
    workspace-root: /Users/yourname/Documents/project

ai:
  provider: real
  api-key: sk-your-api-key
```

**重要**：此文件不提交到 Git。

---

## 第十部分：快速定位代码

### 10.1 我想找到某个 API 的实现

**步骤**：
1. 找到对应的 Controller（如 `CodeManagerController`）
2. 找到对应的方法（如 `scanLocalCode`）
3. 看它调用了哪个 Service
4. 进入 Service 的实现类

**示例**：
```
我想找"扫描目录"的实现
    ↓
CodeManagerController.scanLocalCode()
    ↓
调用了 localFileScanService.scanLocalCodeDir()
    ↓
进入 LocalFileScanService.scanLocalCodeDir()
```

### 10.2 我想找到某个数据库操作

**步骤**：
1. 找到对应的 Repository（如 `CodeSnippetRepository`）
2. 查看定义的方法

**示例**：
```
我想找"根据路径查询代码片段"
    ↓
CodeSnippetRepository.findByFilePath()
```

### 10.3 我想找到某个业务逻辑

**步骤**：
1. 找到对应的 Service 接口
2. 进入 impl 包下的实现类

**示例**：
```
我想找"搜索功能"的业务逻辑
    ↓
SearchService（接口）
    ↓
SearchServiceImpl（实现）
```

### 10.4 我想找到错误处理逻辑

**位置**：`common/config/GlobalExceptionHandler.java`

### 10.5 我想找到配置读取逻辑

**位置**：`common/config/CodeKitProperties.java`

---

## 第十一部分：常见问题排查

### 11.1 API 返回错误码

**排查步骤**：
1. 查看 `ErrorCode.java` 找到错误码含义
2. 查看日志找到具体错误信息
3. 根据错误信息定位代码

### 11.2 数据库操作失败

**排查步骤**：
1. 检查数据库连接配置
2. 检查实体类字段映射
3. 查看日志中的 SQL 语句

### 11.3 缓存问题

**排查步骤**：
1. 检查 Redis 是否启动
2. 检查 Redis 配置
3. 查看缓存 Key 是否正确

### 11.4 文件扫描失败

**排查步骤**：
1. 检查目录是否存在
2. 检查是否有读取权限
3. 查看扫描状态（`/api/code/scan/status`）

---

## 第十二部分：代码统计

### 12.1 文件数量

| 模块 | 文件数 |
|------|--------|
| common | 10 |
| code | 15 |
| search | 8 |
| ai | 7 |
| system | 2 |
| **总计** | **42** |

### 12.2 核心类行数

| 类名 | 行数 | 说明 |
|------|------|------|
| LocalFileScanService | ~600 | 最复杂的服务 |
| CodeSnippetRepository | ~180 | 查询方法最多 |
| GlobalExceptionHandler | ~240 | 异常处理最全 |
| ErrorCode | ~180 | 错误码定义 |
| CodeManagerController | ~210 | API 接口最多 |

---

## 总结

通过这份文档，你应该能够：

1. **理解项目结构**：知道每个模块的职责
2. **定位代码**：知道某个功能在哪个文件
3. **理解数据流**：知道请求是如何被处理的
4. **排查问题**：知道错误是如何被处理的

**建议的学习路径**：

1. 先看 `common` 模块，理解基础设施
2. 再看 `code` 模块，理解核心业务
3. 然后看 `search` 模块，理解检索逻辑
4. 最后看 `ai` 模块，理解 AI 接口

**推荐的调试方式**：

1. 启动项目
2. 用 Postman 测试 API
3. 在关键方法打断点
4. 观察数据流动

如果有任何不清楚的地方，随时可以问我！
