# CodeKit 多轮对话实现（单用户・内存 + JSON 持久化・傻瓜式）

> 目标：实现“同一个 `sessionId` 记住上下文”，并且**重启服务后还能恢复会话**。  
> 项目设定：单用户，不需要 `userId`，只按 `sessionId` 区分会话。

---

## 0. 先看最终效果（你要做到的）

1. 首次提问不传 `sessionId`，后端自动生成并回传。  
2. 后续提问带同一个 `sessionId`，AI 记住上下文。  
3. 服务重启后，会话还能继续（JSON 恢复）。  
4. 点击“新对话”会清空该会话历史。  

---

## 1. 新增会话消息模型

新建文件：`src/main/java/org/itfjnu/codekit/ai/model/ChatMessage.java`

```java
package org.itfjnu.codekit.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role;      // user / assistant
    private String content;
    private LocalDateTime time;
}
```

---

## 2. 定义会话服务接口（返回 Boolean，不用 void）

修改或新建：`src/main/java/org/itfjnu/codekit/ai/service/SessionHistoryService.java`

```java
package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.model.ChatMessage;

import java.util.List;

public interface SessionHistoryService {

    Boolean appendUserMessage(String sessionId, String content);

    Boolean appendAssistantMessage(String sessionId, String content);

    List<ChatMessage> getRecentMessages(String sessionId, int maxRounds);

    Boolean clearSession(String sessionId);
}
```

---

## 3. 关键实现：内存 + JSON 持久化

新建或替换：`src/main/java/org/itfjnu/codekit/ai/service/impl/SessionHistoryServiceImpl.java`

```java
package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.model.ChatMessage;
import org.itfjnu.codekit.ai.service.SessionHistoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionHistoryServiceImpl implements SessionHistoryService {

    private static final int MAX_MESSAGES_PER_SESSION = 20;
    private static final int MAX_SINGLE_MESSAGE_CHARS = 8000;
    private static final long SESSION_TTL_MINUTES = 30;

    // 你的 JSON 文件路径，可按需改
    @Value("${ai.session.store-file:./data/ai-sessions.json}")
    private String storeFile;

    private final ObjectMapper objectMapper;

    private final Map<String, Deque<ChatMessage>> sessionStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromDisk();
    }

    @Override
    public Boolean appendUserMessage(String sessionId, String content) {
        append(sessionId, "user", content);
        return Boolean.TRUE;
    }

    @Override
    public Boolean appendAssistantMessage(String sessionId, String content) {
        append(sessionId, "assistant", content);
        return Boolean.TRUE;
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId, int maxRounds) {
        cleanupExpiredSessions();
        Deque<ChatMessage> deque = sessionStore.get(sessionId);
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }

        int maxMessages = Math.max(1, maxRounds) * 2;
        List<ChatMessage> all = new ArrayList<>(deque);
        int from = Math.max(0, all.size() - maxMessages);
        return new ArrayList<>(all.subList(from, all.size()));
    }

    @Override
    public Boolean clearSession(String sessionId) {
        boolean removed = sessionStore.remove(sessionId) != null;
        flushToDiskSafe();
        return removed;
    }

    private void append(String sessionId, String role, String content) {
        cleanupExpiredSessions();
        Deque<ChatMessage> deque = sessionStore.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        synchronized (deque) {
            String safe = content == null ? "" : content;
            if (safe.length() > MAX_SINGLE_MESSAGE_CHARS) {
                safe = safe.substring(0, MAX_SINGLE_MESSAGE_CHARS);
            }
            deque.addLast(new ChatMessage(role, safe, LocalDateTime.now()));
            while (deque.size() > MAX_MESSAGES_PER_SESSION) {
                deque.removeFirst();
            }
        }
        flushToDiskSafe();
    }

    private void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionStore.entrySet().removeIf(entry -> {
            Deque<ChatMessage> deque = entry.getValue();
            ChatMessage last = deque.peekLast();
            if (last == null || last.getTime() == null) {
                return true;
            }
            return Duration.between(last.getTime(), now).toMinutes() >= SESSION_TTL_MINUTES;
        });
    }

    // 每 60 秒兜底落盘一次，防止进程异常中断导致数据丢失
    @Scheduled(fixedDelay = 60000)
    public void periodicFlush() {
        flushToDiskSafe();
    }

    private void loadFromDisk() {
        try {
            Path path = Paths.get(storeFile);
            if (!Files.exists(path)) {
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) {
                return;
            }
            Map<String, List<ChatMessage>> raw = objectMapper.readValue(
                    bytes, new TypeReference<Map<String, List<ChatMessage>>>() {}
            );
            raw.forEach((sessionId, messages) -> sessionStore.put(sessionId, new ArrayDeque<>(messages)));
            cleanupExpiredSessions();
            log.info("会话历史已从 JSON 恢复，session 数量: {}", sessionStore.size());
        } catch (Exception e) {
            log.warn("会话历史恢复失败，忽略并继续: {}", e.getMessage());
        }
    }

    private void flushToDiskSafe() {
        try {
            Path path = Paths.get(storeFile);
            Files.createDirectories(path.getParent());

            Map<String, List<ChatMessage>> snapshot = new ConcurrentHashMap<>();
            for (Map.Entry<String, Deque<ChatMessage>> entry : sessionStore.entrySet()) {
                snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), snapshot);
        } catch (IOException e) {
            log.error("会话历史写入 JSON 失败: {}", e.getMessage(), e);
        }
    }
}
```

---

## 4. 开启定时任务（必须）

检查 `CodekitApplication` 是否有 `@EnableScheduling`。  
如果没有，改文件：`src/main/java/org/itfjnu/codekit/CodekitApplication.java`

```java
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CodekitApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodekitApplication.class, args);
    }
}
```

---

## 5. 扩展 AI 响应，回传 sessionId

修改：`src/main/java/org/itfjnu/codekit/ai/dto/AIChatResponse.java`  
新增字段：

```java
@Schema(description = "会话ID")
private String sessionId;
```

---

## 6. 在 RealAIServiceImpl 接入多轮上下文

修改：`src/main/java/org/itfjnu/codekit/ai/service/impl/RealAIServiceImpl.java`

### 6.1 增加依赖

```java
private final SessionHistoryService sessionHistoryService;
```

### 6.2 `chat()` 逻辑按下面顺序改

1. 读 `sessionId`，为空就 `UUID` 生成。  
2. 把用户消息写入 session。  
3. 取最近 6 轮历史。  
4. 拼装 prompt（system + history + 当前问题）。  
5. 调 AI。  
6. 把 AI 回复写入 session。  
7. 返回 `AIChatResponse` 时回传 `sessionId`。  

示例片段：

```java
String sessionId = request.getSessionId();
if (sessionId == null || sessionId.isBlank()) {
    sessionId = java.util.UUID.randomUUID().toString();
}

String question = request.getQuestion() == null ? "" : request.getQuestion();
sessionHistoryService.appendUserMessage(sessionId, question);

List<ChatMessage> history = sessionHistoryService.getRecentMessages(sessionId, 6);
String prompt = buildChatPromptWithHistory(request, history);
String answer = callDoubaoAPI(prompt);

sessionHistoryService.appendAssistantMessage(sessionId, answer);

return AIChatResponse.builder()
        .answer(answer)
        .sessionId(sessionId)
        .build();
```

---

## 7. 可选但推荐：新增“清空会话”接口

修改：`src/main/java/org/itfjnu/codekit/ai/controller/AIController.java`

```java
@DeleteMapping("/session/{sessionId}")
public ApiResponse<Boolean> clearSession(@PathVariable String sessionId) {
    Boolean cleared = sessionHistoryService.clearSession(sessionId);
    return ApiResponse.success(cleared);
}
```

---

## 8. 前端接入（必须）

1. 页面状态新增 `sessionId`。  
2. chat 请求里带上 `sessionId`。  
3. 收到响应后更新 `sessionId`。  
4. “新对话”按钮清空 `sessionId`，并调用后端清空接口（可选）。  

示例：

```ts
const sessionId = ref<string | null>(null)

const send = async () => {
  const res = await chatApi({
    question: input.value,
    sessionId: sessionId.value
  })
  sessionId.value = res.data.sessionId ?? sessionId.value
}
```

---

## 9. 配置文件增加 JSON 路径（建议）

在 `application-local.yml` 里加：

```yaml
ai:
  session:
    store-file: ./data/ai-sessions.json
```

> 如果你已经用 `@Value("${ai.session.store-file:...}")` 默认值，也可以先不配。

---

## 10. 验证步骤（按顺序执行）

1. 启动后端。  
2. 前端提问一次，不传 `sessionId`。  
3. 看响应是否返回 `sessionId`。  
4. 用这个 `sessionId` 再问一次，检查是否记住上文。  
5. 重启后端。  
6. 用同一个 `sessionId` 再问，检查是否还能记住上文。  
7. 点击“新对话”，确认上下文被清空。  

---

## 11. 常见坑（你最可能遇到）

1. `sessionId` 始终为空：后端没把 `sessionId` 填到 `AIChatResponse`。  
2. 重启后丢历史：JSON 文件路径没权限或父目录没创建。  
3. 会话爆长变慢：`maxRounds` 太大，先固定 `6`。  
4. 历史写盘太频繁：可以改成“每次 append + 定时任务二选一”，先求稳再优化。  

---

## 12. 这版方案为什么适合你（单用户）

1. 不依赖 Redis / MQ / 外部存储，部署最轻。  
2. 内存读写快，聊天体验好。  
3. JSON 能兜底重启恢复，实用性比纯内存高。  
4. 后续要升级到 SQLite/Redis 也平滑（接口层不变，只替换实现）。  

