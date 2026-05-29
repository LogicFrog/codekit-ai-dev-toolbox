package org.itfjnu.codekit.ai.prompt.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.prompt.PromptTemplateType;
import org.itfjnu.codekit.ai.prompt.service.PromptTemplateService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final ResourcePatternResolver resourceResolver;
    private final Map<PromptTemplateType, String> templateCache = new ConcurrentHashMap<>();

    public PromptTemplateServiceImpl(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @PostConstruct
    public void init() {
        for (PromptTemplateType type : PromptTemplateType.values()) {
            String template = loadTemplate(type);
            if (template != null) {
                templateCache.put(type, template);
                log.debug("Loaded prompt template: {}", type.name());
            } else {
                templateCache.put(type, type.getDefaultTemplate());
                log.info("Using built-in default for prompt template: {}", type.name());
            }
        }
        log.info("PromptTemplateService initialized with {} templates", templateCache.size());
    }

    @Override
    public String render(PromptTemplateType type, Map<String, Object> variables) {
        String template = templateCache.get(type);
        if (template == null) {
            template = type.getDefaultTemplate();
        }

        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }

        return result;
    }

    @Override
    public String getRawTemplate(PromptTemplateType type) {
        String template = templateCache.get(type);
        return template != null ? template : type.getDefaultTemplate();
    }

    @Override
    public void reload() {
        templateCache.clear();
        init();
    }

    private String loadTemplate(PromptTemplateType type) {
        try {
            Resource resource = resourceResolver.getResource("classpath:" + type.getResourcePath());
            if (resource.exists() && resource.isReadable()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("Could not load prompt template from resource: {} ({})", type.getResourcePath(), e.getMessage());
        }
        return null;
    }
}
