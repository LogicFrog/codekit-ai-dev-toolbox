package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.config.LLMProvider;
import org.itfjnu.codekit.utils.TokenEstimator;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.dto.DoubaoRequest;
import org.itfjnu.codekit.ai.dto.DoubaoResponse;
import org.itfjnu.codekit.ai.dto.ChatMessage;
import org.itfjnu.codekit.ai.prompt.PromptTemplateType;
import org.itfjnu.codekit.ai.prompt.service.PromptTemplateService;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.ai.service.SessionHistoryService;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.itfjnu.codekit.common.dto.ErrorCode.*;

/**
 * AI 服务实现 - 支持多 LLM 提供商
 * <p>
 * 所有主流 LLM 提供商（豆包/通义千问/ChatGPT/DeepSeek/文心一言）均支持
 * OpenAI 兼容的 API 格式，通过动态切换 baseUrl 和 apiKey 实现提供商切换。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealAIServiceImpl implements AIService {

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final SessionHistoryService sessionHistoryService;
    private final PromptTemplateService promptTemplateService;

    private static final List<String> SUGGESTION_KEYWORDS = List.of(
            "建议", "改进", "优化", "注意", "可以"
    );
    private static final int DEFAULT_CONTEXT_TOKEN_BUDGET = 4096;

    private volatile RestClient restClient;

    @PostConstruct
    public void init() {
        LLMProvider provider = LLMProvider.fromCode(aiProperties.getProvider());
        log.info("=== AI 服务初始化 ===");
        log.info("提供商: {} ({})", provider.getDisplayName(), provider.getCode());
        log.info("API 地址: {}", aiProperties.getBaseUrl());
        log.info("模型: {}", aiProperties.getModel());
        log.info("超时时间: {} ms", aiProperties.getTimeout());
        log.info("最大 Tokens: {}", aiProperties.getMaxTokens());
        log.info("温度参数: {}", aiProperties.getTemperature());
        log.info("配置状态: {}", aiProperties.isConfigured() ? "已配置" : "未配置");

        buildRestClient();
    }

    public void reconfigure() {
        LLMProvider provider = LLMProvider.fromCode(aiProperties.getProvider());
        log.info("=== AI 服务重新配置 ===");
        log.info("提供商: {} ({})", provider.getDisplayName(), provider.getCode());
        log.info("API 地址: {}", aiProperties.getBaseUrl());
        log.info("模型: {}", aiProperties.getModel());
        buildRestClient();
    }

    private void buildRestClient() {
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(aiProperties.getTimeout());
                    setReadTimeout(aiProperties.getTimeout());
                }})
                .build();
    }

    @Override
    public AIChatResponse chat(AIChatRequest request) {
        log.info("开始处理 chat 请求，问题: {}", truncate(request.getQuestion(), 50));

        if (!aiProperties.isConfigured()) {
            log.warn("AI 服务未配置 API Key");
            throw new ServiceException(CONFIG_ERROR, "AI 未配置。请在设置页面中配置 API Key 和模型");
        }

        String sessionId = resolveSessionId(request.getSessionId());
        String currentQuestion = request.getQuestion() == null ? "" : request.getQuestion();

        int contextBudget = aiProperties.getMaxTokens() > 0 ? aiProperties.getMaxTokens() / 2 : DEFAULT_CONTEXT_TOKEN_BUDGET;
        List<ChatMessage> history = sessionHistoryService.getRecentMessagesByTokenBudget(sessionId, contextBudget);

        String prompt = buildChatPromptWithHistory(request, history);
        String answer = callLLMAPI(prompt);

        int promptTokens = TokenEstimator.estimateTokens(prompt);
        int completionTokens = TokenEstimator.estimateTokens(answer);
        log.info("chat 请求完成 — Token: {}",
                TokenEstimator.formatTokenUsage(promptTokens, completionTokens, aiProperties.getMaxTokens()));

        sessionHistoryService.appendUserMessage(sessionId, currentQuestion);
        sessionHistoryService.appendAssistantMessage(sessionId, answer);

        return AIChatResponse.builder()
                .answer(answer)
                .sessionId(sessionId)
                .build();
    }

    @Override
    public SseEmitter chatStream(AIChatRequest request) {
        if (!aiProperties.isConfigured()) {
            throw new ServiceException(CONFIG_ERROR, "AI 未配置。请在设置页面中配置 API Key 和模型");
        }

        String sessionId = resolveSessionId(request.getSessionId());
        int contextBudget = aiProperties.getMaxTokens() > 0 ? aiProperties.getMaxTokens() / 2 : DEFAULT_CONTEXT_TOKEN_BUDGET;
        List<ChatMessage> history = sessionHistoryService.getRecentMessagesByTokenBudget(sessionId, contextBudget);
        String prompt = buildChatPromptWithHistory(request, history);

        SseEmitter emitter = new SseEmitter((long) aiProperties.getTimeout() + 30000L);
        Thread.startVirtualThread(() -> {
            String userQuestion = request.getQuestion() == null ? "" : request.getQuestion();
            StringBuilder fullAnswer = new StringBuilder();
            try {
                log.info("开始流式 chat，请求 sessionId={}", sessionId);
                callLLMAPIStream(prompt, chunk -> {
                    fullAnswer.append(chunk);
                    sendStreamEvent(emitter, "chunk", Map.of(
                            "content", chunk,
                            "sessionId", sessionId
                    ));
                });

                sessionHistoryService.appendUserMessage(sessionId, userQuestion);
                sessionHistoryService.appendAssistantMessage(sessionId, fullAnswer.toString());

                sendStreamEvent(emitter, "done", Map.of(
                        "sessionId", sessionId,
                        "answer", fullAnswer.toString()
                ));
                log.info("流式 chat 完成，sessionId={}, answerLength={}, promptTokens={}",
                        sessionId, fullAnswer.length(), TokenEstimator.estimateTokens(prompt));
                emitter.complete();
            } catch (Exception e) {
                log.error("流式 chat 处理失败: {}", e.getMessage(), e);
                try {
                    sendStreamEvent(emitter, "error", Map.of(
                            "message", e.getMessage(),
                            "sessionId", sessionId
                    ));
                } catch (Exception ignored) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @Override
    public AIChatResponse explain(AIChatRequest request) {
        log.info("开始处理 explain 请求，代码语言: {}", request.getLanguageType());

        if (!aiProperties.isConfigured()) {
            log.warn("API Key 未配置");
            throw new ServiceException(CONFIG_ERROR, "API Key 未配置。请在设置页面中配置 API Key 和模型");
        }

        String prompt = buildExplainPrompt(request);
        String answer = callLLMAPI(prompt);

        List<String> suggestions = extractSuggestions(answer);

        log.info("explain 请求处理成功，提取到 {} 条建议", suggestions.size());
        return AIChatResponse.builder()
                .answer(answer)
                .suggestions(suggestions)
                .build();
    }

    @Override
    public String getProviderName() {
        return LLMProvider.fromCode(aiProperties.getProvider()).getDisplayName();
    }

    /**
     * 调用 LLM API（OpenAI 兼容格式）
     */
    private String callLLMAPI(String prompt) {
        try {
            DoubaoRequest doubaoRequest = DoubaoRequest.ofUser(
                    aiProperties.getModel(),
                    prompt,
                    aiProperties.getMaxTokens()
            );
            doubaoRequest.setTemperature(aiProperties.getTemperature());

            log.debug("发送请求到LLM API: {}", objectMapper.writeValueAsString(doubaoRequest));

            String currentKey = aiProperties.getApiKey();
            log.info(">>> LLM请求 - Key是否为空: {}, Key长度: {}, Key前缀: {}",
                    currentKey == null || currentKey.isEmpty(),
                    currentKey == null ? 0 : currentKey.length(),
                    (currentKey != null && currentKey.length() >= 8) ? currentKey.substring(0, 8) : "(空)");

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + currentKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(doubaoRequest)
                    .exchange((request, response) -> {
                        int statusCode = response.getStatusCode().value();
                        log.info("LLM响应状态码: {}", statusCode);

                        try (InputStream inputStream = response.getBody()) {
                            if (inputStream == null) {
                                log.error("LLM API 返回空 body，状态码: {}", statusCode);
                                throw new BusinessException(AI_EMPTY_RESPONSE, "状态码: " + statusCode);
                            }

                            byte[] responseBytes = inputStream.readAllBytes();
                            if (responseBytes.length == 0) {
                                log.error("LLM API 返回空内容，状态码: {}", statusCode);
                                throw new BusinessException(AI_EMPTY_RESPONSE, "状态码: " + statusCode + "，响应为空");
                            }

                            String body = new String(responseBytes, StandardCharsets.UTF_8);
                            log.debug("LLM API 原始响应: {}", body);

                            return body;
                        } catch (IOException e) {
                            throw new BusinessException(AI_RESPONSE_READ_FAILED, "读取LLM API 响应失败: " + e.getMessage(), e);
                        }
                    });


            DoubaoResponse response = objectMapper.readValue(responseBody, DoubaoResponse.class);
            
            if (response.getContent() == null || response.getContent().isBlank()) {
                log.error("LLM API 返回空内容");
                throw new BusinessException(AI_EMPTY_RESPONSE, responseBody);
            }

            log.info("LLM API 调用成功，Token 使用: {}", 
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : "未知");
            
            return response.getContent();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP 请求失败: {}", e.getMessage(), e);
            throw new BusinessException(AI_REQUEST_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("处理LLM API 响应失败: {}", e.getMessage(), e);
            throw new BusinessException(AI_RESPONSE_PARSE_FAILED, e.getMessage(), e);
        }
    }

    private void callLLMAPIStream(String prompt, Consumer<String> chunkConsumer) {
        try {
            DoubaoRequest streamRequest = DoubaoRequest.ofUser(
                    aiProperties.getModel(),
                    prompt,
                    aiProperties.getMaxTokens()
            );
            streamRequest.setStream(Boolean.TRUE);
            streamRequest.setTemperature(aiProperties.getTemperature());

            restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(streamRequest)
                    .exchange((req, res) -> {
                        log.info("LLM流式响应状态码: {}", res.getStatusCode());
                        if (res.getStatusCode().isError()) {
                            throw new BusinessException(AI_REQUEST_FAILED, "流式请求失败，状态码: " + res.getStatusCode());
                        }
                        try (InputStream body = res.getBody();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring(5).trim();
                                if (data.isEmpty()) {
                                    continue;
                                }
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                JsonNode root = objectMapper.readTree(data);
                                JsonNode deltaContent = root.path("choices").path(0).path("delta").path("content");
                                if (deltaContent.isTextual() && !deltaContent.asText().isEmpty()) {
                                    chunkConsumer.accept(deltaContent.asText());
                                    continue;
                                }
                                JsonNode messageContent = root.path("choices").path(0).path("message").path("content");
                                if (messageContent.isTextual() && !messageContent.asText().isEmpty()) {
                                    chunkConsumer.accept(messageContent.asText());
                                }
                            }
                            return null;
                        } catch (IOException e) {
                            throw new BusinessException(AI_RESPONSE_READ_FAILED, "流式响应读取失败: " + e.getMessage(), e);
                        }
                    });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(AI_REQUEST_FAILED, "流式调用失败: " + e.getMessage(), e);
        }
    }

    private void sendStreamEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new BusinessException(AI_STREAM_INTERRUPTED, "SSE 推送失败: " + e.getMessage(), e);
        }
    }


    /**
     * 构建 chat 提示词（带多轮历史）
     */
    private String buildChatPromptWithHistory(AIChatRequest request, List<ChatMessage> history) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(promptTemplateService.render(PromptTemplateType.CHAT_SYSTEM, null));
        prompt.append("\n\n");

        if (history != null && !history.isEmpty()) {
            prompt.append("以下是最近对话上下文（按时间顺序）：\n");
            for (ChatMessage message : history) {
                if ("user".equals(message.getRole())) {
                    prompt.append("用户：").append(message.getContent()).append('\n');
                } else if ("assistant".equals(message.getRole())) {
                    prompt.append("助手：").append(message.getContent()).append('\n');
                }
            }
            prompt.append('\n');
        }

        if (request.getCode() != null && !request.getCode().isEmpty()) {
            prompt.append("当前代码：\n```\n")
                    .append(request.getCode())
                    .append("\n```\n\n");
        }

        prompt.append("当前问题：")
                .append(request.getQuestion() == null ? "" : request.getQuestion());

        return prompt.toString();
    }

    private String resolveSessionId(String rawSessionId) {
        if (rawSessionId == null || rawSessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return rawSessionId;
    }

    /**
     * 构建 explain 提示词
     */
    private String buildExplainPrompt(AIChatRequest request) {
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("languageType", request.getLanguageType() != null ? request.getLanguageType() : "");
        vars.put("code", request.getCode() != null ? request.getCode() : "");
        return promptTemplateService.render(PromptTemplateType.CODE_EXPLAIN, vars);
    }

    private String buildOptimizePrompt(AIChatRequest request, String optimizeType) {
        PromptTemplateType type = switch (optimizeType) {
            case "performance" -> PromptTemplateType.CODE_OPTIMIZE_PERFORMANCE;
            case "readability" -> PromptTemplateType.CODE_OPTIMIZE_READABILITY;
            case "bugfix" -> PromptTemplateType.CODE_OPTIMIZE_BUGFIX;
            default -> PromptTemplateType.CODE_OPTIMIZE_ALL;
        };
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("languageType", request.getLanguageType() != null ? request.getLanguageType() : "Java");
        vars.put("code", request.getCode() != null ? request.getCode() : "");
        return promptTemplateService.render(type, vars);
    }

    /**
     * 从 AI 回答中提取建议
     *
     * 提取逻辑：
     * 1. 按行拆分回答
     * 2. 找包含"建议""改进""优化"等关键词的行
     * 3. 去掉开头的序号（如 "1. "、"- "）
     * 4. 最多保留 3 条
     *
     * @param answer AI 的回答
     * @return 建议列表
     */

    private List<String> extractSuggestions(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> suggestions = new ArrayList<>();
        String[] lines = answer.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();

            if (SUGGESTION_KEYWORDS.stream().anyMatch(trimmed::contains)) {
                // 去掉开头的序号
                String cleanLine = trimmed.replaceAll("^[\\d\\-\\*\\.\\s]+", "");

                // 去掉过长的行（可能是完整句子而不是建议）
                if (cleanLine.length() < 100 && !cleanLine.isEmpty()) {
                    suggestions.add(cleanLine);
                }

                // 最多保留 3 条
                if (suggestions.size() >= 3) {
                    break;
                }
            }
        }
        return suggestions;
    }

    @Override
    public AIChatResponse optimize(AIChatRequest request) {
        log.info("开始处理 optimize 请求，代码语言: {}", request.getLanguageType());

        if (!aiProperties.isConfigured()) {
            log.warn("API Key 未配置");
            throw new ServiceException(CONFIG_ERROR, "API Key 未配置。请在设置页面中配置 API Key 和模型");
        }

        String optimizeType = "all";
        String question = request.getQuestion();
        if (question != null) {
            String lower = question.toLowerCase();
            if (lower.contains("性能") || lower.contains("performance")) {
                optimizeType = "performance";
            } else if (lower.contains("可读性") || lower.contains("readability")) {
                optimizeType = "readability";
            } else if (lower.contains("bug") || lower.contains("修复")) {
                optimizeType = "bugfix";
            }
        }

        String prompt = buildOptimizePrompt(request, optimizeType);
        String answer = callLLMAPI(prompt);

        List<String> suggestions = extractSuggestions(answer);

        log.info("optimize 请求处理成功，优化类型: {}", optimizeType);
        return AIChatResponse.builder()
                .answer(answer)
                .suggestions(suggestions)
                .build();
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
