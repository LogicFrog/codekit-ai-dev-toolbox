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
     *
     * @param mockService 模拟服务实例
     * @param realService 真实服务实例
     * @return 返回选中的服务实例
     */
    @Bean
    public AIService aiService(MockAIServiceImpl mockService, RealAIServiceImpl realService) {
        String provider = aiProperties.getProvider();

        log.info("AI Provider 配置：{}", provider);

        // 忽略大小写比较，支持 "real"、"Real"、"REAL" 等多种写法
        if ("real".equalsIgnoreCase(provider)) {
            log.info("使用 RealAIServiceImpl（真实 AI 服务）");
            log.info("API Key 配置状态：{}", aiProperties.isConfigured() ? "✅ 已配置" : "❌ 未配置");
            return realService;
        }

        log.info("使用 MockAIServiceImpl（模拟 AI 服务）");
        return mockService;
    }
}
