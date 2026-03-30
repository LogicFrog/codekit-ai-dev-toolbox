package org.itfjnu.codekit.ai.service;

import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;

public interface AIService {
    AIChatResponse chat(AIChatRequest request);
    AIChatResponse explain(AIChatRequest request);
    String getProviderName();
}
