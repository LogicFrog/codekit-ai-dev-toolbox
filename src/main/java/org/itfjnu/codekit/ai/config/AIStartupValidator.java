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
        boolean isRealProvider = !"mock".equalsIgnoreCase(provider);

        if (isRealProvider && !aiProperties.isConfigured()) {
            log.warn("AI provider={} 已启用但 API Key 未配置，请在设置页面中配置", provider);
        }

        if (!StringUtils.hasText(embeddingApi)) {
            log.warn("语义检索未配置 ai.embedding-api，语义检索功能将不可用");
        }
        if (!StringUtils.hasText(embeddingModel)) {
            log.warn("语义检索未配置 ai.embedding-model，语义检索功能将不可用");
        }
        if (!StringUtils.hasText(embeddingApiKey)) {
            log.warn("语义检索未配置 ai.embedding-api-key（或 ai.api-key），语义检索功能将不可用");
        }

        if (StringUtils.hasText(embeddingApi) && StringUtils.hasText(embeddingModel) && StringUtils.hasText(embeddingApiKey)) {
            log.info("AI 启动校验通过，provider={}, embeddingModel={}", provider, embeddingModel);
        }
    }
}
