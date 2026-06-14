package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Consumer;

public interface AIService {
    AIChatResponse chat(AIChatRequest request);
    SseEmitter chatStream(AIChatRequest request);
    AIChatResponse explain(AIChatRequest request);
    String getProviderName();
    AIChatResponse optimize(AIChatRequest request);

    default AIChatResponse explainStream(AIChatRequest request, Consumer<String> chunkConsumer) {
        return explain(request);
    }

    default AIChatResponse optimizeStream(AIChatRequest request, Consumer<String> chunkConsumer) {
        return optimize(request);
    }
}
