package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIService {
    AIChatResponse chat(AIChatRequest request);
    SseEmitter chatStream(AIChatRequest request);
    AIChatResponse explain(AIChatRequest request);
    String getProviderName();
}
