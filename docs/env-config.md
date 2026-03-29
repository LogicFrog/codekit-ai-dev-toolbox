# CodeKit 阶段一：基础环境配置说明

本项目（CodeKit）是一个基于 JDK 21、Spring Boot 3、Vue 3 及 AI Agent 技术的智能代码辅助中枢。为确保项目在本地顺利运行，请按以下步骤配置基础环境。

## 1. 软件版本要求

| 组件 | 推荐版本 | 说明 |
| :--- | :--- | :--- |
| **JDK** | 21 (LTS) | 必须使用 JDK 21+ 以支持 **虚拟线程 (Virtual Threads)**。 |
| **MySQL** | 8.0 及以上 | 核心数据存储，推荐使用 8.0.30+。 |
| **Redis** | 6.x / 7.x | 缓存、热点数据存储、对话上下文暂存。 |
| **Milvus Lite** | 最新版 | 向量数据库，用于 RAG 语义检索（轻量化部署）。 |
| **Node.js** | 18.x 及以上 | 前端 Vue 3 工程打包及运行环境。 |

## 2. 环境安装与配置步骤

### 2.1 JDK 21 安装
- 从 [Adoptium](https://adoptium.net/temurin/releases/?version=21) 下载 JDK 21。
- 配置 `JAVA_HOME` 环境变量。
- 验证：`java -version`。

### 2.2 MySQL 8.0 配置
1. 创建数据库：
   ```sql
   CREATE DATABASE codekit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 初始化表结构：执行项目目录下的 `docs/db-init.sql` 脚本。
3. 修改后端 `application.yml` 中的数据库配置（默认为 `root/12345678`）。

### 2.3 Redis 配置
- 本地安装 Redis Server 或使用 Docker 运行：
  ```bash
  docker run -d --name codekit-redis -p 6379:6379 redis:latest
  ```

### 2.4 Milvus Lite 配置
- 后端集成 Milvus Lite，启动时会自动初始化本地持久化存储（默认为 `docs/milvus.db`）。

## 3. 前端工程配置
1. 进入前端目录：`cd web/codekit-web`
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`

---

## 4. 常见问题 (FAQ)
- **虚拟线程不生效？** 确保 `application.yml` 中 `spring.threads.virtual.enabled` 为 `true`。
- **MySQL 连接超时？** 检查数据库服务是否已启动，且 `allowPublicKeyRetrieval=true` 参数已在 JDBC URL 中配置。
