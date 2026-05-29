package org.itfjnu.codekit.ai.agent.skill.impl;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionListSkillImplTest {

    @Mock
    private VersionInfoService versionInfoService;

    @InjectMocks
    private VersionListSkillImpl skill;

    @Test
    @DisplayName("name() 返回 version_list")
    void name_ReturnsVersionList() {
        assertEquals("version_list", skill.name());
    }

    @Test
    @DisplayName("params 有 snippetId 直接查询")
    void execute_SnippetIdInParams_QueriesDirectly() {
        VersionInfo v1 = createVersion(1L, "v1.0", "初始版本");
        VersionInfo v2 = createVersion(2L, "v1.1", "修复 Bug");
        when(versionInfoService.listVersions(10L)).thenReturn(List.of(v1, v2));

        Map<String, Object> params = Map.of("snippetId", 10L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(2, data.get("count"));
        assertEquals(10L, data.get("snippetId"));
    }

    @Test
    @DisplayName("params 无 snippetId 时从上下文 search_top_id 获取")
    void execute_SnippetIdFromContext() {
        VersionInfo v1 = createVersion(1L, "v1.0", "desc");
        when(versionInfoService.listVersions(99L)).thenReturn(List.of(v1));

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_id", 99L);

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("缺少 snippetId 且上下文无 search_top_id 返回失败")
    void execute_NoSnippetId_ReturnsFailure() {
        Map<String, Object> params = new HashMap<>();

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("snippetId"));
    }

    @Test
    @DisplayName("写入上下文 version_count 和 version_items")
    void execute_WritesContext() {
        VersionInfo v1 = createVersion(1L, "v1.0", "初始");
        VersionInfo v2 = createVersion(2L, "v2.0", "更新");
        VersionInfo v3 = createVersion(3L, "v3.0", "修复");
        when(versionInfoService.listVersions(5L)).thenReturn(List.of(v1, v2, v3));

        Map<String, Object> params = Map.of("snippetId", 5L);
        Map<String, Object> context = new HashMap<>();

        skill.execute(params, context);

        assertEquals(3, context.get("version_count"));
        assertEquals(List.of(v1, v2, v3), context.get("version_items"));
    }

    @Test
    @DisplayName("空版本列表也能正确返回")
    void execute_EmptyVersionList_ReturnsSuccess() {
        when(versionInfoService.listVersions(5L)).thenReturn(List.of());

        Map<String, Object> params = Map.of("snippetId", 5L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(0, data.get("count"));
    }

    @Test
    @DisplayName("异常时捕获并返回失败")
    void execute_Exception_ReturnsFailure() {
        when(versionInfoService.listVersions(1L)).thenThrow(new RuntimeException("版本服务异常"));

        Map<String, Object> params = Map.of("snippetId", 1L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("版本服务异常"));
    }

    private VersionInfo createVersion(Long id, String name, String desc) {
        VersionInfo v = new VersionInfo();
        v.setId(id);
        v.setVersionName(name);
        v.setDescription(desc);
        return v;
    }
}
