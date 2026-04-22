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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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


    // 每个会话最多保留消息条数（user+assistant 一起算）
    private static final int MAX_MESSAGES_PER_SESSION = 20;
    // 单条消息最大长度，避免超长内容撑爆内存/token
    private static final int MAX_SINGLE_MESSAGE_CHARS = 8000;
    // 会话 TTL：30 分钟不活跃自动清理
    private static final long SESSION_TTL_MINUTES = 30;

    @Value("${ai.session.store-file:./data/ai-sessions.json}")
    private String storeFile;

    private final ObjectMapper objectMapper;

    private final Map<String, Deque<ChatMessage>> sessionStore = new ConcurrentHashMap<>();
    private final Object fileWriteLock = new Object();

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
        synchronized (fileWriteLock) {
            try {
                Path path = Paths.get(storeFile);
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Map<String, List<ChatMessage>> snapshot = new ConcurrentHashMap<>();
                for (Map.Entry<String, Deque<ChatMessage>> entry : sessionStore.entrySet()) {
                    snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }

                Path tmpPath = path.resolveSibling(path.getFileName() + ".tmp");
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), snapshot);

                try {
                    Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.error("会话历史写入 JSON 失败: {}", e.getMessage(), e);
            }
        }
    }

}
