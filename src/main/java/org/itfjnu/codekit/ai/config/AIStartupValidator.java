package org.itfjnu.codekit.ai.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIStartupValidator {

    private final AIProperties aiProperties;

    @Value("${ai.embedding-api:}")
    private String embeddingApi;

    @Value("${ai.embedding-model:}")
    private String embeddingModel;

    @Value("${ai.embedding-api-key:${ai.api-key:}}")
    private String embeddingApiKey;

    @PostConstruct
    public void validate() {
        String provider = aiProperties.getProvider();
        boolean realProvider = "real".equalsIgnoreCase(provider);

        if (realProvider) {
            if (!aiProperties.isConfigured()) {
                throw new IllegalStateException("AI provider=real 时必须配置 ai.api-key");
            }
            if (!StringUtils.hasText(aiProperties.getModel())) {
                throw new IllegalStateException("AI provider=real 时必须配置 ai.model");
            }
        }

        if (!StringUtils.hasText(embeddingApi)) {
            throw new IllegalStateException("语义检索必须配置 ai.embedding-api");
        }
        if (!StringUtils.hasText(embeddingModel)) {
            throw new IllegalStateException("语义检索必须配置 ai.embedding-model");
        }
        if (!StringUtils.hasText(embeddingApiKey)) {
            throw new IllegalStateException("语义检索必须配置 ai.embedding-api-key（或 ai.api-key）");
        }

        log.info("AI 启动校验通过，provider={}, embeddingModel={}", provider, embeddingModel);
    }
}
