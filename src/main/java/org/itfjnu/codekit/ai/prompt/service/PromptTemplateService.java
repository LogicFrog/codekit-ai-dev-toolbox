package org.itfjnu.codekit.ai.prompt.service;

import org.itfjnu.codekit.ai.prompt.PromptTemplateType;

import java.util.Map;

public interface PromptTemplateService {

    String render(PromptTemplateType type, Map<String, Object> variables);

    String getRawTemplate(PromptTemplateType type);

    void reload();
}
