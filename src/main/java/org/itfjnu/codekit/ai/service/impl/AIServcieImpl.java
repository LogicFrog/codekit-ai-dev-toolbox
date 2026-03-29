package org.itfjnu.codekit.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIServcieImpl implements AIService {
    @Override
    public AIChatResponse chat(AIChatRequest request) {
        return null;
    }

    @Override
    public AIChatResponse explain(AIChatRequest request) {
        return null;
    }
}
