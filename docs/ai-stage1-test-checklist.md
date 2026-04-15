# AI 第一阶段测试清单（2026-04-13）

## 已完成（自动化）

### 1) 服务层单元测试
- 文件：`src/test/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImplTest.java`
- 覆盖点：
  - API Key 未配置时 `chat()` 抛出 `ServiceException(CONFIG_ERROR)`
  - API Key 未配置时 `explain()` 抛出 `ServiceException(CONFIG_ERROR)`
  - `getProviderName()` 返回 `real`
  - 空问题 / null 问题 / null 代码边界行为
  - `sessionId` 字段在请求对象中保持不变

### 2) 控制器层单元测试
- 文件：`src/test/java/org/itfjnu/codekit/ai/controller/AIControllerTest.java`
- 覆盖点：
  - `/api/ai/chat` 控制器方法请求透传 + 成功响应包装
  - `/api/ai/explain` 控制器方法请求透传 + 建议列表响应

## 执行命令

```bash
export JAVA_HOME=/Users/annu/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
./mvnw -q test -Dtest=org.itfjnu.codekit.ai.service.impl.RealAIServiceImplTest,org.itfjnu.codekit.ai.controller.AIControllerTest
```

## 还没覆盖（建议下一步）

- 真实 API 联调测试（依赖可用 API Key）
- 前端 AI 页面端到端交互测试
- 多轮会话上下文测试（你准备自己实现后补）
