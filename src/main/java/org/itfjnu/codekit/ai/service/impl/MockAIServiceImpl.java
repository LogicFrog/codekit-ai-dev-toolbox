package org.itfjnu.codekit.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;

import java.util.List;

@Slf4j
public class MockAIServiceImpl implements AIService {

    @Override
    public AIChatResponse chat(AIChatRequest request) {
        log.info("MockAIServiceImpl - chat: {}", request.getQuestion());
        
        return AIChatResponse.builder()
                .answer("【Mock模式】你问的是：" + request.getQuestion())
                .suggestions(List.of(
                        "你可以尝试更具体地描述问题",
                        "提供代码片段可以获得更准确的回答"
                ))
                .codeBlocks(null)
                .error(null)
                .build();
    }

    @Override
    public AIChatResponse explain(AIChatRequest request) {
        log.info("MockAIServiceImpl - explain: {}", request.getLanguageType());
        
        String code = request.getCode() != null ? request.getCode() : "";
        String truncated = code.length() > 200 ? code.substring(0, 200) + "..." : code;
        
        return AIChatResponse.builder()
                .answer("【Mock模式】这是一段 " + request.getLanguageType() + " 代码。\n\n代码摘要：\n" + truncated)
                .suggestions(List.of(
                        "建议添加注释提高可读性",
                        "考虑添加单元测试"
                ))
                .codeBlocks(List.of(
                        AIChatResponse.CodeBlock.builder()
                                .language(request.getLanguageType())
                                .code("// 优化示例\n// TODO: 待真实AI生成")
                                .description("优化建议示例")
                                .build()
                ))
                .error(null)
                .build();
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
