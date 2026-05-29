package org.itfjnu.codekit.ai.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.itfjnu.codekit.ai.prompt.service.PromptTemplateService;
import org.itfjnu.codekit.ai.prompt.service.impl.PromptTemplateServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private ResourcePatternResolver resourceResolver;

    @Mock
    private Resource resource;

    private PromptTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromptTemplateServiceImpl(resourceResolver);
    }

    @Test
    @DisplayName("初始化时加载默认模板")
    void init_UsesDefaultTemplate_WhenResourceNotFound() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        service.init();

        String template = service.getRawTemplate(PromptTemplateType.CHAT_SYSTEM);
        assertNotNull(template);
        assertTrue(template.contains("CodeKit"));
    }

    @Test
    @DisplayName("渲染模板替换占位符变量")
    void render_ReplacesPlaceholders() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        service.init();

        Map<String, Object> vars = new HashMap<>();
        vars.put("languageType", "Java");
        vars.put("code", "public class Hello {}");

        String result = service.render(PromptTemplateType.CODE_EXPLAIN, vars);

        assertTrue(result.contains("Java"));
        assertTrue(result.contains("public class Hello {}"));
        assertFalse(result.contains("{languageType}"));
        assertFalse(result.contains("{code}"));
    }

    @Test
    @DisplayName("渲染模板变量为 null 时替换为空字符串")
    void render_NullVariable_ReplacedWithEmpty() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        service.init();

        Map<String, Object> vars = new HashMap<>();
        vars.put("languageType", null);
        vars.put("code", "test");

        String result = service.render(PromptTemplateType.CODE_EXPLAIN, vars);

        assertFalse(result.contains("{languageType}"));
        assertTrue(result.contains("test"));
    }

    @Test
    @DisplayName("渲染模板空变量 map 返回原始模板")
    void render_EmptyVariables_ReturnsRawTemplate() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        service.init();

        String rawTemplate = service.getRawTemplate(PromptTemplateType.CODE_EXPLAIN);
        String rendered = service.render(PromptTemplateType.CODE_EXPLAIN, new HashMap<>());

        assertEquals(rawTemplate, rendered);
    }

    @Test
    @DisplayName("渲染模板 null 变量 map 返回原始模板")
    void render_NullVariables_ReturnsRawTemplate() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        service.init();

        String rawTemplate = service.getRawTemplate(PromptTemplateType.CODE_EXPLAIN);
        String rendered = service.render(PromptTemplateType.CODE_EXPLAIN, null);

        assertEquals(rawTemplate, rendered);
    }

    @Test
    @DisplayName("getRawTemplate 返回缓存模板或默认模板")
    void getRawTemplate_ReturnsCachedOrDefault() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        service.init();

        String template = service.getRawTemplate(PromptTemplateType.VERSION_ANALYZE);

        assertNotNull(template);
        assertTrue(template.contains("代码评审"));
    }

    @Test
    @DisplayName("reload 清空缓存并重新加载")
    void reload_ClearsCacheAndReloads() throws Exception {
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);
        service.init();

        service.reload();

        String template = service.getRawTemplate(PromptTemplateType.CHAT_SYSTEM);
        assertNotNull(template);
    }

    @Test
    @DisplayName("所有 PromptTemplateType 都有非空默认模板")
    void allPromptTypes_HaveDefaultTemplate() {
        for (PromptTemplateType type : PromptTemplateType.values()) {
            assertNotNull(type.getDefaultTemplate(), type.name() + " should have default template");
            assertFalse(type.getDefaultTemplate().isBlank(), type.name() + " default template should not be blank");
        }
    }
}
