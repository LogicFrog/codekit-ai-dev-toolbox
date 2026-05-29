package org.itfjnu.codekit.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.ai.service.impl.MockAIServiceImpl;
import org.itfjnu.codekit.ai.service.impl.RealAIServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIConfig {

    private final AIProperties aiProperties;

    @Bean
    public MockAIServiceImpl mockAIService() {
        return new MockAIServiceImpl();
    }

    /**
     * 根据 provider 配置选择 AI 服务实现
     * <p>
     * 支持 doubao / qwen / openai / deepseek / wenxin / real(向后兼容=豆包)
     * mock 模式用于开发调试
     *
     * @param mockService 模拟服务实例
     * @param realService 真实服务实例
     * @return 返回选中的服务实例
     */
    @Bean
    public AIService aiService(MockAIServiceImpl mockService, RealAIServiceImpl realService) {
        String provider = aiProperties.getProvider();

        log.info("AI Provider 配置：{}", provider);

        if ("mock".equalsIgnoreCase(provider)) {
            log.info("使用 MockAIServiceImpl（模拟 AI 服务）");
            return mockService;
        }

        LLMProvider llmProvider = LLMProvider.fromCode(provider);
        log.info("使用 RealAIServiceImpl → {} ({})", llmProvider.getDisplayName(), llmProvider.getCode());

        if (!aiProperties.isConfigured()) {
            log.warn("API Key 未配置，可在设置页面中配置");
        }

        return realService;
    }
}
