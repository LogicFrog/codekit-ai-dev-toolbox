package org.itfjnu.codekit.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.ai.service.impl.MockAIServiceImpl;
import org.itfjnu.codekit.ai.service.impl.RealAIServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AIConfig {

    @Value("${ai.provider:mock}")
    private String providerType;

    @Bean
    public MockAIServiceImpl mockAIService() {
        return new MockAIServiceImpl();
    }

    @Bean
    public RealAIServiceImpl realAIService() {
        return new RealAIServiceImpl();
    }

    @Bean
    public AIService aiService(MockAIServiceImpl mockService, RealAIServiceImpl realService) {
        log.info("AI Provider 配置: {}", providerType);
        
        if ("real".equals(providerType)) {
            log.info("使用 RealAIServiceImpl");
            return realService;
        }
        
        log.info("使用 MockAIServiceImpl");
        return mockService;
    }
}
