# CodeKit 阶段一：系统设计文档

## 1. 架构概览
CodeKit 采用前后端分离的架构模式，阶段一主要完成基础工程搭建与核心数据库设计。

- **后端**: Spring Boot 3.x + JDK 21 (虚拟线程池)。
- **前端**: Vue 3.x + Vite + TypeScript + Element Plus。
- **数据库**: MySQL 8.0 (元数据) + Redis (缓存)。

## 2. 数据库设计 (MySQL)
基于阶段一的需求，核心表结构如下：

### 2.1 代码片段表 (`code_snippet`)
存储本地扫描后的代码片段元信息。
- `id`: 主键 ID。
- `file_path`: 文件绝对路径 (唯一索引)。
- `file_name`: 文件名。
- `code_content`: 代码内容。
- `language_type`: 语言类型 (Java/Python/JS 等)。
- `tag`: 用户自定义标签。

### 2.2 代码依赖表 (`code_dependency`)
记录代码片段之间的依赖关系。
- `snippet_id`: 关联 `code_snippet` 的 ID。
- `dependency_name`: 依赖项名称。
- `version`: 依赖版本。

### 2.3 版本信息表 (`version_info`)
存储代码的版本快照。
- `snippet_id`: 关联 `code_snippet` 的 ID。
- `code_snapshot`: 代码内容快照。

### 2.4 检索历史表 (`search_history`)
记录用户的检索行为，用于后续优化检索精度。

## 3. 后端接口设计 (REST API)
阶段一核心接口列表：

| 接口名称 | 请求方法 | 路径 | 描述 |
| :--- | :--- | :--- | :--- |
| **文件扫描** | POST | `/api/fs/scan` | 触发本地文件系统扫描 |
| **代码片段查询** | GET | `/api/code/snippets` | 分页查询已扫描的代码片段 |
| **代码片段详情** | GET | `/api/code/snippets/{id}` | 获取单个代码片段的详细内容 |
| **依赖关系查询** | GET | `/api/code/dependencies` | 查询指定代码片段的依赖列表 |

## 4. 前端布局设计
- **导航栏**: 集成项目 Logo、全局搜索框、用户配置入口。
- **侧边栏**: 功能切换（代码管理、AI 助手、检索中心、版本管理）。
- **主内容区**: 使用 `Monaco Editor` 展示代码，提供多页签切换能力。

---

## 5. 技术决策
- **JDK 21 虚拟线程**: 通过 `spring.threads.virtual.enabled=true` 开启，提升 I/O 密集型任务（如文件扫描、AI 流式响应）的并发性能。
- **CORS 全局配置**: 解决前后端联调中的跨域问题。
