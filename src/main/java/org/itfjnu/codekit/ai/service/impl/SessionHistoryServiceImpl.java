package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.utils.TokenEstimator;
import org.itfjnu.codekit.ai.dto.ChatMessage;
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

    private static final int MAX_MESSAGES_PER_SESSION = 20;
    private static final int MAX_SINGLE_MESSAGE_CHARS = 8000;
    private static final long SESSION_TTL_MINUTES = 30;
    private static final int DEFAULT_CONTEXT_TOKEN_BUDGET = 4096;
    private static final int DEFAULT_CONTEXT_ROUNDS = 4;

    @Value("${ai.session.store-file:./data/ai-sessions.json}")
    private String storeFile;

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;

    private final Map<String, Deque<ChatMessage>> sessionStore = new ConcurrentHashMap<>();
    private final Object fileWriteLock = new Object();

    @PostConstruct
    public void init() {
        loadFromDisk();
    }

    private int maxContextTokens() {
        int val = aiProperties.getMaxContextTokens();
        return val > 0 ? val : DEFAULT_CONTEXT_TOKEN_BUDGET;
    }

    private int maxContextRounds() {
        int val = aiProperties.getMaxContextRounds();
        return val > 0 ? val : DEFAULT_CONTEXT_ROUNDS;
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
    public List<ChatMessage> getRecentMessagesByTokenBudget(String sessionId, int maxTokens) {
        cleanupExpiredSessions();
        Deque<ChatMessage> deque = sessionStore.get(sessionId);
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }

        int budget = maxTokens > 0 ? maxTokens : maxContextTokens();
        List<ChatMessage> all = new ArrayList<>(deque);
        List<ChatMessage> selected = new ArrayList<>();
        int usedTokens = 0;

        for (int i = all.size() - 1; i >= 0; i--) {
            ChatMessage msg = all.get(i);
            int msgTokens = msg.getEstimatedTokens();
            if (usedTokens + msgTokens > budget) {
                if (!selected.isEmpty()) {
                    break;
                }
                String truncated = TokenEstimator.truncateToTokenBudget(msg.getContent(), budget);
                ChatMessage truncatedMsg = new ChatMessage(msg.getRole(), truncated, msg.getTime());
                selected.add(0, truncatedMsg);
                break;
            }
            usedTokens += msgTokens;
            selected.add(0, msg);
        }

        if (!selected.isEmpty()) {
            log.debug("Token-aware context: {}/{} tokens, {} messages selected from {} total",
                    usedTokens, budget, selected.size(), all.size());
        }
        return selected;
    }

    @Override
    public int getSessionTokenUsage(String sessionId) {
        Deque<ChatMessage> deque = sessionStore.get(sessionId);
        if (deque == null || deque.isEmpty()) {
            return 0;
        }
        return deque.stream().mapToInt(ChatMessage::getEstimatedTokens).sum();
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
            ChatMessage message = new ChatMessage(role, safe, LocalDateTime.now());
            deque.addLast(message);

            int sessionTokens = deque.stream().mapToInt(ChatMessage::getEstimatedTokens).sum();
            if (sessionTokens > maxContextTokens() * 3) {
                deque.removeFirst();
                log.debug("Session {} exceeded max tokens, pruned oldest message", sessionId);
            }

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
