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
---

## 4. API Key 安全配置

### 4.1 环境变量注入（推荐）
CodeKit 支持通过环境变量注入所有敏感配置，**切勿将 API Key 直接写在配置文件中**。

| 环境变量 | 说明 |
|---|---|
| `CODEKIT_AI_API_KEY` | AI 大模型 API Key（豆包/通义千问/ChatGPT 等） |
| `CODEKIT_AI_EMBEDDING_API_KEY` | Embedding 向量化 API Key（为空时自动复用上面的主 Key） |
| `CODEKIT_AI_PROVIDER` | 提供商代码：doubao / qwen / openai / deepseek / wenxin |
| `CODEKIT_AI_MODEL` | 模型名称 |
| `CODEKIT_VAULT_PASSWORD` | 加密保险库主密码（用于加密本地存储的 API Key，可选） |

**设置方式：**

macOS / Linux:
```bash
export CODEKIT_AI_API_KEY="your-api-key-here"
export CODEKIT_AI_PROVIDER="doubao"
./mvnw spring-boot:run
```

Windows (PowerShell):
```powershell
$env:CODEKIT_AI_API_KEY="your-api-key-here"
$env:CODEKIT_AI_PROVIDER="doubao"
mvnw spring-boot:run
```

### 4.2 前端 Settings 页面持久化
API Key 通过前端 Settings 页面保存后：
- 使用 **AES-256-GCM** 加密存储在 `data/ai-settings.json`
- 密钥派生使用 PBKDF2WithHmacSHA256（10,000 次迭代）
- 返回前端时自动脱敏显示（如 `97ab****c7`）
- 支持按不同 AI 提供商分别保存独立的 API Key

### 4.3 加密保险库
`data/ai-settings.json` 中的 API Key 已加密。加密主密码优先级：
1. **环境变量 `CODEKIT_VAULT_PASSWORD`**（推荐设置）
2. 自动派生密钥（基于本机 hostname，更换机器后需重新输入 API Key）

### 4.4 安全检查清单
- [ ] API Key 不直接写入 `application-local.yml`
- [ ] `application-local.yml` 已加入 `.gitignore`
- [ ] 生产环境使用环境变量注入密钥
- [ ] 设置了 `CODEKIT_VAULT_PASSWORD` 以加固本地加密

---

## 5. 常见问题 (FAQ)
- **虚拟线程不生效？** 确保 `application.yml` 中 `spring.threads.virtual.enabled` 为 `true`。
- **MySQL 连接超时？** 检查数据库服务是否已启动，且 `allowPublicKeyRetrieval=true` 参数已在 JDBC URL 中配置。
- **API Key 在哪里设置？** 推荐通过环境变量 `CODEKIT_AI_API_KEY` 注入，或通过前端 Settings → AI 设置页面输入。
- **换了电脑/机器后 API Key 丢失？** 如果未设置 `CODEKIT_VAULT_PASSWORD`，加密密钥与机器 hostname 绑定。换机器后需要重新输入 API Key。
