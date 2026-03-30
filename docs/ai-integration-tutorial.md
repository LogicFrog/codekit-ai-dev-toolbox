# AI 模块真功能接入教学文档

## 写在前面

这份文档会手把手教你如何把 CodeKit 的 AI 模块从一个"空壳"变成真正能调用大模型的实用功能。

**你不需要有任何 AI 开发经验**，只需要跟着文档一步步做即可。

---

## 第一部分：基础概念理解

在开始写代码之前，我们需要先理解几个核心概念。如果你已经了解这些，可以跳过。

### 1.1 什么是 API？

**API（Application Programming Interface，应用程序编程接口）** 可以理解为"程序的遥控器"。

举个例子：
- 你在餐厅点餐，你不需要自己去厨房做菜
- 你只需要告诉服务员你要什么
- 服务员会把你的需求传达给厨房
- 厨房做好后，服务员把菜端给你

在这个比喻中：
- **你** = 我们的程序（CodeKit）
- **服务员** = API
- **厨房** = AI 模型服务（如 DeepSeek、OpenAI）

我们只需要"告诉 API 我们要什么"，API 会帮我们完成和 AI 模型的交互。

### 1.2 什么是 HTTP 请求？

**HTTP 请求** 是程序之间通信的方式，就像发短信一样。

HTTP 请求有几种类型，最常用的是：
- **GET**：获取数据（比如"给我看看你的用户列表"）
- **POST**：发送数据（比如"帮我处理这段文字"）

我们调用 AI 模型，用的是 **POST 请求**，因为我们要"发送问题给 AI，让它回答"。

### 1.3 什么是 JSON？

**JSON（JavaScript Object Notation）** 是一种数据格式，用来在不同程序之间传递结构化数据。

它长这样：
```json
{
  "name": "张三",
  "age": 25,
  "skills": ["Java", "Python", "MySQL"]
}
```

你可以把它理解为一个"键值对"的集合：
- `"name"` 是键，`"张三"` 是值
- `"skills"` 是键，`["Java", "Python", "MySQL"]` 是值（这是一个数组）

我们调用 AI API 时，请求和响应都是 JSON 格式。

### 1.4 什么是 API Key？

**API Key** 就像是一把"钥匙"，证明你有权限使用某个服务。

当你注册 DeepSeek 或 OpenAI 账号后，平台会给你一个 API Key，类似于：
```
sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

每次调用 API 时，你都需要带上这个 Key，否则服务会拒绝你的请求。

**重要提醒**：
- API Key 是敏感信息，绝对不能提交到 Git 仓库
- 不要在代码里硬编码 API Key
- 要从环境变量或配置文件中读取

### 1.5 什么是大语言模型（LLM）？

**大语言模型（Large Language Model）** 是一种 AI，它能够理解和生成人类语言。

常见的模型有：
- **GPT-4**（OpenAI）- 最知名，但国内访问需要特殊手段
- **DeepSeek**（国产）- 国内可直接访问，价格便宜，性能不错
- **Claude**（Anthropic）- 擅长长文本处理
- **通义千问**（阿里）- 国产，中文能力强

本教程使用 **DeepSeek**，因为：
1. 国内可以直接访问，不需要翻墙
2. 价格便宜（约 1 元/百万 token）
3. API 格式与 OpenAI 兼容，学会后可以无缝切换

---

## 第二部分：准备工作

### 2.1 注册 DeepSeek 账号并获取 API Key

**步骤 1**：访问 DeepSeek 官网
- 网址：https://platform.deepseek.com/

**步骤 2**：注册账号
- 点击右上角"注册"
- 可以用手机号或微信注册

**步骤 3**：创建 API Key
- 登录后，点击右上角头像 → "API Keys"
- 点击"创建 API Key"
- 给 Key 起个名字（比如 "codekit"）
- 复制生成的 API Key（类似 `sk-xxxxxxxxxx`）

**重要**：API Key 只会显示一次，一定要保存好！

**步骤 4**：充值（可选）
- 新用户通常有免费额度
- 如果没有，可以充值 10 元，足够测试使用

### 2.2 测试 API 是否可用

在写代码之前，我们可以先用命令行测试一下 API 是否正常工作。

打开终端，执行以下命令（把 `YOUR_API_KEY` 替换成你的真实 Key）：

```bash
curl https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {"role": "user", "content": "你好，请用一句话介绍自己"}
    ]
  '
```

如果成功，你会看到类似这样的响应：
```json
{
  "id": "chatcmpl-xxx",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "你好！我是 DeepSeek AI，一个由 DeepSeek 公司开发的大语言模型..."
      }
    }
  ]
}
```

如果看到这个响应，说明 API 可以正常使用了！

---

## 第三部分：理解现有代码结构

在开始修改代码之前，我们需要先了解现有的代码结构。

### 3.1 现有文件结构

```
src/main/java/org/itfjnu/codekit/ai/
├── controller/
│   └── AIController.java          # 接收前端请求的控制器
├── dto/
│   ├── AIChatRequest.java         # 请求的数据结构
│   └── AIChatResponse.java        # 响应的数据结构
└── service/
    ├── AIService.java             # 服务接口（定义了要做什么）
    └── impl/
        ├── MockAIServiceImpl.java # 模拟实现（返回假数据）
        └── RealAIServiceImpl.java # 真实实现（目前是空的，我们要填充它）
```

### 3.2 数据流理解

```
前端页面（AIAssistant.vue）
    ↓ 用户输入问题
AIController（接收 HTTP 请求）
    ↓ 封装成 AIChatRequest
AIService（根据配置选择实现）
    ↓ 调用具体实现
MockAIServiceImpl 或 RealAIServiceImpl
    ↓ 返回 AIChatResponse
前端页面（显示回答）
```

### 3.3 现有接口定义

**AIChatRequest.java**（请求）：
```java
public class AIChatRequest {
    private String question;      // 用户的问题
    private String code;          // 代码内容（可选）
    private String languageType;  // 代码语言（可选）
    private String sessionId;     // 会话ID（可选，用于多轮对话）
}
```

**AIChatResponse.java**（响应）：
```java
public class AIChatResponse {
    private String answer;              // AI 的回答
    private List<String> suggestions;   // 建议列表（可选）
    private List<CodeBlock> codeBlocks; // 代码块（可选）
    private String error;               // 错误信息（如果有）
    
    public static class CodeBlock {
        private String language;    // 代码语言
        private String code;        // 代码内容
        private String description; // 代码说明
    }
}
```

**AIService.java**（接口）：
```java
public interface AIService {
    AIChatResponse chat(AIChatRequest request);    // 通用对话
    AIChatResponse explain(AIChatRequest request); // 代码解释
    String getProviderName();                      // 返回提供者名称
}
```

### 3.4 现有 RealAIServiceImpl.java

目前这个类是空的，只是返回错误信息：

```java
public class RealAIServiceImpl implements AIService {
    @Override
    public AIChatResponse chat(AIChatRequest request) {
        return AIChatResponse.builder()
                .answer("真实AI功能尚未配置...")
                .error("AI_PROVIDER_NOT_CONFIGURED")
                .build();
    }
    // ...
}
```

我们的任务就是让这个类真正调用 AI API。

---

## 第四部分：创建配置类

### 4.1 为什么需要配置类？

我们需要配置以下信息：
- API Key（密钥）
- 模型名称（如 deepseek-chat）
- API 地址（如 https://api.deepseek.com）
- 超时时间

这些信息不应该写死在代码里，而应该从配置文件中读取。

### 4.2 创建 AIProperties.java

**文件位置**：`src/main/java/org/itfjnu/codekit/ai/config/AIProperties.java`

**完整代码**：

```java
package org.itfjnu.codekit.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置类
 * 
 * 这个类会自动读取 application.yml 中以 "ai" 开头的配置项
 * 例如：ai.api-key 会被读取到 apiKey 字段
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    /**
     * API 密钥
     * 从环境变量 CODEKIT_AI_API_KEY 读取
     * 如果没有配置，默认为空字符串
     */
    private String apiKey = "";

    /**
     * 模型名称
     * 例如：deepseek-chat, gpt-4, gpt-3.5-turbo
     * 默认使用 deepseek-chat
     */
    private String model = "deepseek-chat";

    /**
     * API 基础地址
     * DeepSeek: https://api.deepseek.com
     * OpenAI: https://api.openai.com
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * 请求超时时间（毫秒）
     * 默认 30 秒
     */
    private int timeout = 30000;

    /**
     * 最大 token 数
     * 控制返回内容的长度
     */
    private int maxTokens = 2048;

    /**
     * 检查配置是否有效
     * @return true 表示配置有效，false 表示缺少必要配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
```

### 4.3 代码详解

**@ConfigurationProperties(prefix = "ai")**
- 这个注解告诉 Spring：把 `application.yml` 中以 `ai.` 开头的配置项，自动映射到这个类的字段
- 例如：`ai.api-key` → `apiKey`，`ai.model` → `model`

**@Component**
- 让 Spring 管理这个类，可以在其他地方通过 `@Autowired` 注入使用

**isConfigured() 方法**
- 检查 API Key 是否已配置
- 后面会用到这个方法来判断是否可以调用 API

---

## 第五部分：更新配置文件

### 5.1 修改 application.yml

**文件位置**：`src/main/resources/application.yml`

**在文件末尾添加**：

```yaml
ai:
  provider: ${CODEKIT_AI_PROVIDER:mock}
  api-key: ${CODEKIT_AI_API_KEY:}
  model: ${CODEKIT_AI_MODEL:deepseek-chat}
  base-url: ${CODEKIT_AI_BASE_URL:https://api.deepseek.com}
  timeout: ${CODEKIT_AI_TIMEOUT:30000}
  max-tokens: ${CODEKIT_AI_MAX_TOKENS:2048}
```

**解释**：
- `${CODEKIT_AI_API_KEY:}` 的意思是：
  - 优先读取环境变量 `CODEKIT_AI_API_KEY`
  - 如果环境变量不存在，使用默认值（冒号后面是默认值，这里是空字符串）

### 5.2 修改 application-local.yml

**文件位置**：`src/main/resources/application-local.yml`

**添加你的真实配置**：

```yaml
ai:
  provider: real
  api-key: sk-your-real-api-key-here
  model: deepseek-chat
  base-url: https://api.deepseek.com
  timeout: 30000
  max-tokens: 2048
```

**重要**：
- 把 `sk-your-real-api-key-here` 替换成你从 DeepSeek 获取的真实 API Key
- `application-local.yml` 不会提交到 Git（已在 .gitignore 中）

### 5.3 确认 .gitignore

检查 `.gitignore` 文件，确保包含：

```
src/main/resources/application-local.yml
```

这样可以防止你的 API Key 被提交到 Git 仓库。

---

## 第六部分：理解 HTTP 客户端

### 6.1 Spring Boot 中的 HTTP 客户端

Spring Boot 提供了多种发送 HTTP 请求的方式：
1. **RestTemplate** - 传统方式，简单易用
2. **WebClient** - 响应式，功能更强大
3. **RestClient** - Spring 6.1 新增，推荐使用

本教程使用 **RestClient**，因为它：
- 是 Spring 6.1+ 的推荐方式
- API 简洁现代
- 支持同步和异步

### 6.2 RestClient 基本用法

```java
// 1. 创建 RestClient
RestClient restClient = RestClient.create();

// 2. 发送 POST 请求
String response = restClient.post()
    .uri("https://api.example.com/chat")           // 请求地址
    .header("Authorization", "Bearer " + apiKey)   // 请求头
    .contentType(MediaType.APPLICATION_JSON)       // 内容类型
    .body(requestBody)                             // 请求体
    .retrieve()                                    // 执行请求
    .body(String.class);                           // 响应类型

// 3. 解析响应
// response 是 JSON 字符串，需要解析成 Java 对象
```

### 6.3 DeepSeek API 请求格式

DeepSeek 使用 OpenAI 兼容的 API 格式：

**请求**：
```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "你是一个代码助手"},
    {"role": "user", "content": "请解释这段代码：public class Hello {}"}
  ],
  "max_tokens": 2048
}
```

**响应**：
```json
{
  "id": "chatcmpl-xxx",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "这是一个简单的 Java 类定义..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 20,
    "completion_tokens": 50,
    "total_tokens": 70
  }
}
```

---

## 第七部分：创建请求和响应的 DTO 类

为了方便处理 API 请求和响应，我们需要创建对应的 Java 类。

### 7.1 创建 DeepSeekRequest.java

**文件位置**：`src/main/java/org/itfjnu/codekit/ai/dto/DeepSeekRequest.java`

```java
package org.itfjnu.codekit.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DeepSeek API 请求体
 * 使用 OpenAI 兼容格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekRequest {

    /**
     * 模型名称
     * 例如：deepseek-chat
     */
    private String model;

    /**
     * 消息列表
     * 包含 system、user、assistant 等角色的消息
     */
    private List<Message> messages;

    /**
     * 最大 token 数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 消息对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色：system（系统提示）、user（用户）、assistant（AI助手）
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }

    /**
     * 快速创建用户消息
     */
    public static DeepSeekRequest ofUser(String model, String content, Integer maxTokens) {
        return DeepSeekRequest.builder()
                .model(model)
                .messages(List.of(
                        Message.builder()
                                .role("user")
                                .content(content)
                                .build()
                ))
                .maxTokens(maxTokens)
                .build();
    }

    /**
     * 创建带系统提示的请求
     */
    public static DeepSeekRequest ofSystemAndUser(String model, String systemPrompt, 
                                                   String userContent, Integer maxTokens) {
        return DeepSeekRequest.builder()
                .model(model)
                .messages(List.of(
                        Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        Message.builder()
                                .role("user")
                                .content(userContent)
                                .build()
                ))
                .maxTokens(maxTokens)
                .build();
    }
}
```

### 7.2 创建 DeepSeekResponse.java

**文件位置**：`src/main/java/org/itfjnu/codekit/ai/dto/DeepSeekResponse.java`

```java
package org.itfjnu.codekit.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DeepSeek API 响应体
 * 使用 OpenAI 兼容格式
 */
@Data
public class DeepSeekResponse {

    /**
     * 响应 ID
     */
    private String id;

    /**
     * 响应选择列表
     * 通常只有一个选择
     */
    private List<Choice> choices;

    /**
     * Token 使用情况
     */
    private Usage usage;

    /**
     * 选择对象
     */
    @Data
    public static class Choice {
        /**
         * 选择索引
         */
        private Integer index;

        /**
         * 消息内容
         */
        private Message message;

        /**
         * 结束原因
         * stop: 正常结束
         * length: 达到最大长度
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 消息对象
     */
    @Data
    public static class Message {
        /**
         * 角色：assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }

    /**
     * Token 使用统计
     */
    @Data
    public static class Usage {
        /**
         * 提示词 token 数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 生成 token 数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总 token 数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 获取 AI 回复内容
     * @return AI 的回复文本，如果没有则返回 null
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            Message message = choices.get(0).getMessage();
            if (message != null) {
                return message.getContent();
            }
        }
        return null;
    }
}
```

---

## 第八部分：实现 RealAIServiceImpl

这是最核心的部分，我们来实现真正的 AI 调用。

### 8.1 完整代码

**文件位置**：`src/main/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImpl.java`

```java
package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.dto.DeepSeekRequest;
import org.itfjnu.codekit.ai.dto.DeepSeekResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * 真实 AI 服务实现
 * 
 * 这个类负责真正调用 AI API（如 DeepSeek、OpenAI）
 * 并将结果转换为我们的标准响应格式
 */
@Slf4j
@Service("realAIService")
@RequiredArgsConstructor
public class RealAIServiceImpl implements AIService {

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;

    /**
     * 创建 RestClient 实例
     * RestClient 是 Spring 6.1+ 推荐的 HTTP 客户端
     */
    private RestClient createRestClient() {
        return RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .build();
    }

    /**
     * 通用对话
     * 
     * @param request 用户请求，包含问题和可选的代码
     * @return AI 的回答
     */
    @Override
    public AIChatResponse chat(AIChatRequest request) {
        log.info("开始处理 chat 请求，问题: {}", truncate(request.getQuestion(), 50));

        // 1. 检查配置
        if (!aiProperties.isConfigured()) {
            log.warn("AI API Key 未配置");
            return AIChatResponse.builder()
                    .answer("AI 功能未配置。请在 application-local.yml 中设置 ai.api-key")
                    .error("API_KEY_NOT_CONFIGURED")
                    .build();
        }

        // 2. 构建提示词
        String prompt = buildChatPrompt(request);
        log.debug("构建的提示词: {}", prompt);

        // 3. 调用 AI API
        try {
            String answer = callDeepSeekAPI(prompt);
            log.info("chat 请求处理成功");

            return AIChatResponse.builder()
                    .answer(answer)
                    .build();

        } catch (Exception e) {
            log.error("调用 AI API 失败", e);
            return AIChatResponse.builder()
                    .answer("抱歉，AI 服务暂时不可用，请稍后重试。")
                    .error("API_CALL_FAILED: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 代码解释
     * 
     * @param request 用户请求，主要包含代码和语言类型
     * @return 代码解释结果
     */
    @Override
    public AIChatResponse explain(AIChatRequest request) {
        log.info("开始处理 explain 请求，语言: {}", request.getLanguageType());

        // 1. 检查配置
        if (!aiProperties.isConfigured()) {
            log.warn("AI API Key 未配置");
            return AIChatResponse.builder()
                    .answer("AI 功能未配置。请在 application-local.yml 中设置 ai.api-key")
                    .error("API_KEY_NOT_CONFIGURED")
                    .build();
        }

        // 2. 构建解释专用提示词
        String prompt = buildExplainPrompt(request);
        log.debug("构建的提示词: {}", prompt);

        // 3. 调用 AI API
        try {
            String answer = callDeepSeekAPI(prompt);
            log.info("explain 请求处理成功");

            // 尝试提取建议
            List<String> suggestions = extractSuggestions(answer);

            return AIChatResponse.builder()
                    .answer(answer)
                    .suggestions(suggestions)
                    .build();

        } catch (Exception e) {
            log.error("调用 AI API 失败", e);
            return AIChatResponse.builder()
                    .answer("抱歉，代码解释服务暂时不可用，请稍后重试。")
                    .error("API_CALL_FAILED: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return "real";
    }

    /**
     * 构建 chat 提示词
     */
    private String buildChatPrompt(AIChatRequest request) {
        StringBuilder prompt = new StringBuilder();

        // 如果有代码，先展示代码
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            prompt.append("代码语言：").append(request.getLanguageType()).append("\n\n");
            prompt.append("代码内容：\n```\n");
            prompt.append(request.getCode());
            prompt.append("\n```\n\n");
        }

        // 添加用户问题
        prompt.append(request.getQuestion());

        return prompt.toString();
    }

    /**
     * 构建 explain 提示词
     * 使用更结构化的提示，引导 AI 给出更好的解释
     */
    private String buildExplainPrompt(AIChatRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请解释以下");
        if (request.getLanguageType() != null) {
            prompt.append(request.getLanguageType());
        } else {
            prompt.append("代码");
        }
        prompt.append("代码：\n\n");

        prompt.append("```");
        if (request.getLanguageType() != null) {
            prompt.append(request.getLanguageType().toLowerCase());
        }
        prompt.append("\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");

        prompt.append("请从以下几个方面进行解释：\n");
        prompt.append("1. **功能概述**：这段代码实现了什么功能？\n");
        prompt.append("2. **关键逻辑**：核心算法或逻辑是什么？\n");
        prompt.append("3. **代码结构**：主要的类、方法、变量及其作用。\n");
        prompt.append("4. **改进建议**：有什么可以优化的地方？\n");

        return prompt.toString();
    }

    /**
     * 调用 DeepSeek API
     * 
     * @param prompt 提示词
     * @return AI 的回复
     */
    private String callDeepSeekAPI(String prompt) {
        // 1. 创建请求体
        DeepSeekRequest deepSeekRequest = DeepSeekRequest.ofSystemAndUser(
                aiProperties.getModel(),
                "你是一个专业的编程助手，擅长解释代码、回答技术问题。请用中文回答，回答要清晰、准确、有条理。",
                prompt,
                aiProperties.getMaxTokens()
        );

        // 2. 创建 RestClient
        RestClient restClient = createRestClient();

        // 3. 发送请求
        try {
            String responseJson = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(deepSeekRequest)
                    .retrieve()
                    .body(String.class);

            // 4. 解析响应
            DeepSeekResponse response = objectMapper.readValue(responseJson, DeepSeekResponse.class);
            String content = response.getContent();

            if (content == null || content.isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            log.debug("AI 响应: {}", truncate(content, 200));
            return content;

        } catch (RestClientException e) {
            log.error("HTTP 请求失败: {}", e.getMessage());
            throw new RuntimeException("AI API 调用失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("解析响应失败: {}", e.getMessage());
            throw new RuntimeException("解析 AI 响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从回答中提取建议
     * 简单实现：查找"建议"或"改进"相关的行
     */
    private List<String> extractSuggestions(String answer) {
        List<String> suggestions = new ArrayList<>();
        String[] lines = answer.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.contains("建议") || line.contains("改进") || line.contains("优化")) {
                // 移除开头的数字和点，如 "1. "、"2. "
                line = line.replaceAll("^\\d+\\.\\s*", "");
                if (!line.isEmpty() && line.length() > 5) {
                    suggestions.add(line);
                }
            }
        }

        // 最多返回 3 条建议
        return suggestions.size() > 3 ? suggestions.subList(0, 3) : suggestions;
    }

    /**
     * 截断字符串（用于日志）
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
```

### 8.2 代码详解

#### 8.2.1 依赖注入

```java
@RequiredArgsConstructor
public class RealAIServiceImpl implements AIService {
    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
}
```

- `@RequiredArgsConstructor` 是 Lombok 注解，自动生成构造函数
- `AIProperties` 用于读取配置
- `ObjectMapper` 用于 JSON 序列化/反序列化

#### 8.2.2 创建 RestClient

```java
private RestClient createRestClient() {
    return RestClient.builder()
            .baseUrl(aiProperties.getBaseUrl())  // 设置基础 URL
            .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())  // 设置认证头
            .build();
}
```

- `baseUrl`：API 的基础地址，如 `https://api.deepseek.com`
- `Authorization`：认证头，格式是 `Bearer {api-key}`

#### 8.2.3 发送请求

```java
String responseJson = restClient.post()
        .uri("/chat/completions")              // API 路径
        .contentType(MediaType.APPLICATION_JSON)  // 内容类型
        .body(deepSeekRequest)                 // 请求体
        .retrieve()                            // 执行请求
        .body(String.class);                   // 响应类型
```

- `.post()`：发送 POST 请求
- `.uri()`：API 路径，会拼接到 baseUrl 后面
- `.body()`：请求体，会自动序列化为 JSON
- `.retrieve().body(String.class)`：获取响应体为字符串

#### 8.2.4 解析响应

```java
DeepSeekResponse response = objectMapper.readValue(responseJson, DeepSeekResponse.class);
String content = response.getContent();
```

- 使用 Jackson 的 `ObjectMapper` 将 JSON 字符串转换为 Java 对象
- `getContent()` 是我们封装的便捷方法，直接获取 AI 的回复内容

---

## 第九部分：更新服务选择逻辑

现在我们有了 `RealAIServiceImpl`，还需要让 Spring 根据配置选择使用哪个实现。

### 9.1 查看现有的服务选择逻辑

找到 `AIController` 或服务配置类，看看是如何选择 `MockAIServiceImpl` 和 `RealAIServiceImpl` 的。

### 9.2 创建 AI 服务配置类

**文件位置**：`src/main/java/org/itfjnu/codekit/ai/config/AIServiceConfig.java`

```java
package org.itfjnu.codekit.ai.config;

import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.ai.service.impl.MockAIServiceImpl;
import org.itfjnu.codekit.ai.service.impl.RealAIServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务配置类
 * 
 * 根据 ai.provider 配置项选择使用哪个 AIService 实现：
 * - mock: 使用 MockAIServiceImpl（返回模拟数据）
 * - real: 使用 RealAIServiceImpl（调用真实 AI API）
 */
@Configuration
public class AIServiceConfig {

    @Autowired
    private AIProperties aiProperties;

    @Autowired
    private MockAIServiceImpl mockAIService;

    @Autowired
    private RealAIServiceImpl realAIService;

    /**
     * 根据配置选择 AI 服务实现
     */
    @Bean
    public AIService aiService() {
        String provider = aiProperties.getProvider();
        
        if ("real".equalsIgnoreCase(provider)) {
            return realAIService;
        } else {
            return mockAIService;
        }
    }
}
```

### 9.3 确保 AIProperties 有 provider 字段

在 `AIProperties.java` 中添加：

```java
/**
 * AI 提供者
 * mock: 使用模拟实现
 * real: 使用真实实现
 */
private String provider = "mock";
```

---

## 第十部分：测试

### 10.1 单元测试

创建测试类来验证功能。

**文件位置**：`src/test/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImplTest.java`

```java
package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealAIServiceImplTest {

    @Mock
    private AIProperties aiProperties;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RealAIServiceImpl realAIService;

    private AIChatRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new AIChatRequest();
        testRequest.setQuestion("这段代码是什么意思？");
        testRequest.setCode("public class Hello { public static void main(String[] args) { System.out.println(\"Hello\"); } }");
        testRequest.setLanguageType("Java");
    }

    @Test
    @DisplayName("API Key 未配置时返回错误")
    void testChat_ApiKeyNotConfigured() {
        when(aiProperties.isConfigured()).thenReturn(false);

        AIChatResponse response = realAIService.chat(testRequest);

        assertNotNull(response);
        assertEquals("API_KEY_NOT_CONFIGURED", response.getError());
        verify(aiProperties, times(1)).isConfigured();
    }

    @Test
    @DisplayName("getProviderName 返回 real")
    void testGetProviderName() {
        assertEquals("real", realAIService.getProviderName());
    }
}
```

### 10.2 集成测试

启动应用后，使用 curl 或 Postman 测试：

```bash
# 测试 chat 接口
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是 Spring Boot？",
    "languageType": "Java"
  }'

# 测试 explain 接口
curl -X POST http://localhost:8080/api/ai/explain \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Hello { public static void main(String[] args) { System.out.println(\"Hello\"); } }",
    "languageType": "Java"
  }'
```

### 10.3 前端测试

1. 启动后端服务
2. 启动前端项目
3. 打开 AI 助手页面
4. 输入问题，查看是否能正常返回 AI 回答

---

## 第十一部分：常见问题与解决方案

### 11.1 API Key 配置问题

**问题**：提示 "API_KEY_NOT_CONFIGURED"

**解决方案**：
1. 检查 `application-local.yml` 中是否配置了 `ai.api-key`
2. 确保 `ai.provider` 设置为 `real`
3. 重启应用

### 11.2 网络连接问题

**问题**：提示 "API_CALL_FAILED" 或连接超时

**解决方案**：
1. 检查网络是否能访问 `https://api.deepseek.com`
2. 检查是否需要配置代理
3. 尝试增加超时时间：`ai.timeout: 60000`

### 11.3 API 返回错误

**问题**：API 返回 401 或 403 错误

**解决方案**：
1. 检查 API Key 是否正确
2. 检查 API Key 是否有效（是否过期、是否有余额）
3. 登录 DeepSeek 控制台查看 API Key 状态

### 11.4 响应解析失败

**问题**：提示 "解析 AI 响应失败"

**解决方案**：
1. 检查 DeepSeek API 响应格式是否有变化
2. 查看后端日志，获取原始响应内容
3. 可能需要更新 `DeepSeekResponse` 类

---

## 第十二部分：扩展阅读

### 12.1 如何切换到其他模型

如果你想使用其他模型（如 OpenAI、通义千问），只需要：

1. 修改 `application-local.yml`：
```yaml
ai:
  provider: real
  api-key: your-openai-api-key
  model: gpt-4o-mini
  base-url: https://api.openai.com
```

2. 大多数模型都兼容 OpenAI API 格式，无需修改代码

### 12.2 如何添加多轮对话支持

目前 `sessionId` 字段已预留，但未实现。要添加多轮对话：

1. 创建一个 `Map<String, List<Message>>` 存储会话历史
2. 在调用 API 时，把历史消息也带上
3. 设置过期时间，定期清理过期会话

### 12.3 如何添加流式响应

流式响应可以让用户看到 AI 逐字生成内容，体验更好：

1. 使用 `WebClient` 替代 `RestClient`
2. 设置 `stream: true` 参数
3. 使用 SSE（Server-Sent Events）向前端推送数据

---

## 总结

恭喜你完成了 AI 模块的真功能接入！

你学到了：
1. 如何理解和使用 REST API
2. 如何在 Spring Boot 中发送 HTTP 请求
3. 如何处理 JSON 数据
4. 如何管理敏感配置
5. 如何实现错误处理和降级

下一步可以做的：
1. 添加更多 AI 功能（如代码优化、代码生成）
2. 实现多轮对话
3. 添加流式响应
4. 添加使用量统计

如果有任何问题，欢迎随时提问！
