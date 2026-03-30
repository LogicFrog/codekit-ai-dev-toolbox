package org.itfjnu.codekit.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;

@Slf4j
public class RealAIServiceImpl implements AIService {

    @Override
    public AIChatResponse chat(AIChatRequest request) {
        log.warn("RealAIServiceImpl 未配置，请检查 ai.provider 设置");
        
        return AIChatResponse.builder()
                .answer("真实AI功能尚未配置。请在 application.yml 中设置 ai.provider=real 并配置对应的 API Key。")
                .error("AI_PROVIDER_NOT_CONFIGURED")
                .build();
    }

    @Override
    public AIChatResponse explain(AIChatRequest request) {
        log.warn("RealAIServiceImpl 未配置");
        
        return AIChatResponse.builder()
                .answer("真实AI功能尚未配置。")
                .error("AI_PROVIDER_NOT_CONFIGURED")
                .build();
    }

    @Override
    public String getProviderName() {
        return "real";
    }
}
