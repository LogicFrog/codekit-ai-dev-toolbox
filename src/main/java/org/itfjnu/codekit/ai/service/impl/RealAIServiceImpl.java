package org.itfjnu.codekit.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.config.AIProperties;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.dto.DoubaoRequest;
import org.itfjnu.codekit.ai.dto.DoubaoResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.itfjnu.codekit.common.dto.ErrorCode.*;

/**
 * 真实 AI 服务实现 - 豆包版本
 * 
 * 使用火山方舟 API 调用豆包大模型
 * API 文档：https://www.volcengine.com/docs/82379/1099475
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealAIServiceImpl implements AIService {

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper;

    // 定义关键词
    private static final List<String> SUGGESTION_KEYWORDS = List.of(
            "建议", "改进", "优化", "注意", "可以"
    );

    private RestClient restClient;

    @PostConstruct
    public void init() {
        log.info("=== 豆包 AI 配置信息 ===");
        log.info("API 地址: {}", aiProperties.getBaseUrl());
        log.info("接入点 ID: {}", aiProperties.getModel());
        log.info("超时时间: {} ms", aiProperties.getTimeout());
        log.info("最大 Tokens: {}", aiProperties.getMaxTokens());
        log.info("配置状态: {}", aiProperties.isConfigured() ? "✅ 已配置" : "❌ 未配置");

        if (!aiProperties.isConfigured()) {
            log.warn("豆包 AI 未配置，请检查 application-local.yml 中的 ai.api-key 和 ai.model");
        }

        restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.getApiKey())
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
            log.warn("豆包 AI 未配置");
            throw new ServiceException(CONFIG_ERROR, "豆包 AI 未配置。请在 application-local.yml 中设置 ai.api-key 和 ai.model");
        }

        String prompt = buildChatPrompt(request);
        String answer = callDoubaoAPI(prompt);

        log.info("chat 请求处理成功");
        return AIChatResponse.builder()
                .answer(answer)
                .build();
    }

    @Override
    public AIChatResponse explain(AIChatRequest request) {
        log.info("开始处理 explain 请求，代码语言: {}", request.getLanguageType());

        if (!aiProperties.isConfigured()) {
            log.warn("API Key 未配置");
            throw new ServiceException(CONFIG_ERROR, "API Key 未配置。请在 application-local.yml 中设置 ai.api-key");
        }

        String prompt = buildExplainPrompt(request);
        String answer = callDoubaoAPI(prompt);

        List<String> suggestions = extractSuggestions(answer);

        log.info("explain 请求处理成功，提取到 {} 条建议", suggestions.size());
        return AIChatResponse.builder()
                .answer(answer)
                .suggestions(suggestions)
                .build();
    }

    @Override
    public String getProviderName() {
        return "real";
    }

    /**
     * 调用豆包 API
     * 
     * @param prompt 提示词
     * @return AI 的回复内容
     */
    private String callDoubaoAPI(String prompt) {
        try {
            DoubaoRequest doubaoRequest = DoubaoRequest.ofUser(
                    aiProperties.getModel(),
                    prompt,
                    aiProperties.getMaxTokens()
            );

            log.debug("发送请求到豆包 API: {}", objectMapper.writeValueAsString(doubaoRequest));

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(doubaoRequest)
                    .exchange((request, response) -> {
                        log.info("豆包响应状态码: {}", response.getStatusCode());
                        log.info("豆包响应 Content-Type: {}", response.getHeaders().getContentType());

                        try (InputStream inputStream = response.getBody()) {
                            if (inputStream == null) {
                                throw new BusinessException(AI_EMPTY_RESPONSE);
                            }

                            byte[] responseBytes = inputStream.readAllBytes();
                            if (responseBytes.length == 0) {
                                throw new BusinessException(AI_EMPTY_RESPONSE);
                            }

                            String body = new String(responseBytes, StandardCharsets.UTF_8);
                            log.debug("豆包 API 原始响应: {}", body);

                            return body;
                        } catch (IOException e) {
                            throw new BusinessException(AI_RESPONSE_READ_FAILED, "读取豆包 API 响应失败: " + e.getMessage(), e);
                        }
                    });


            DoubaoResponse response = objectMapper.readValue(responseBody, DoubaoResponse.class);
            
            if (response.getContent() == null || response.getContent().isBlank()) {
                log.error("豆包 API 返回空内容");
                throw new BusinessException(AI_EMPTY_RESPONSE, responseBody);
            }

            log.info("豆包 API 调用成功，Token 使用: {}", 
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : "未知");
            
            return response.getContent();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP 请求失败: {}", e.getMessage(), e);
            throw new BusinessException(AI_REQUEST_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("处理豆包 API 响应失败: {}", e.getMessage(), e);
            throw new BusinessException(AI_RESPONSE_PARSE_FAILED, e.getMessage(), e);
        }
    }


    /**
     * 构建 chat 提示词
     */
    private String buildChatPrompt(AIChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            prompt.append("代码：\n```\n")
                  .append(request.getCode())
                  .append("\n```\n\n");
        }
        
        prompt.append(request.getQuestion());
        
        return prompt.toString();
    }

    /**
     * 构建 explain 提示词
     */
    private String buildExplainPrompt(AIChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请详细解释以下");
        if (request.getLanguageType() != null && !request.getLanguageType().isEmpty()) {
            prompt.append(request.getLanguageType()).append(" ");
        }
        prompt.append("代码：\n\n");
        
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            prompt.append("```\n")
                  .append(request.getCode())
                  .append("\n```\n\n");
        }
        
        prompt.append("请从以下几个方面进行解释：\n");
        prompt.append("1. 代码的主要功能和目的\n");
        prompt.append("2. 关键逻辑和算法\n");
        prompt.append("3. 重要的类、方法和变量\n");
        prompt.append("4. 可能的改进建议（如果有）");
        
        return prompt.toString();
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
