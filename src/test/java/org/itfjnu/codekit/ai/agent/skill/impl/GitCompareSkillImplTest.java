package org.itfjnu.codekit.ai.agent.skill.impl;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.code.dto.VersionAnalyzeRequest;
import org.itfjnu.codekit.code.dto.VersionAnalyzeResponse;
import org.itfjnu.codekit.code.dto.VersionDiffResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitCompareSkillImplTest {

    @Mock
    private VersionInfoService versionInfoService;

    @InjectMocks
    private GitCompareSkillImpl skill;

    @Test
    @DisplayName("name() 返回 git_compare")
    void name_ReturnsGitCompare() {
        assertEquals("git_compare", skill.name());
    }

    @Test
    @DisplayName("指定两个版本 ID 进行对比和 AI 分析")
    void execute_SpecifiedVersions_ComparesAndAnalyzes() {
        VersionInfo v1 = createVersion(1L, "v1.0");
        VersionInfo v2 = createVersion(2L, "v2.0");
        when(versionInfoService.listVersions(10L)).thenReturn(List.of(v1, v2));

        VersionDiffResponse diffResponse = new VersionDiffResponse();
        diffResponse.setSummary("+ line added\n- line removed");
        when(versionInfoService.compareVersions(eq(10L), eq(1L), eq(2L))).thenReturn(diffResponse);

        VersionAnalyzeResponse analyzeResponse = new VersionAnalyzeResponse();
        analyzeResponse.setSummary("无风险");
        when(versionInfoService.analyzeVersions(eq(10L), any(VersionAnalyzeRequest.class))).thenReturn(analyzeResponse);

        Map<String, Object> params = new HashMap<>();
        params.put("snippetId", 10L);
        params.put("versionA", 1L);
        params.put("versionB", 2L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(10L, data.get("snippetId"));
        assertEquals(1L, data.get("versionAId"));
        assertEquals(2L, data.get("versionBId"));
        assertEquals(diffResponse, data.get("diff"));
        assertEquals(analyzeResponse, data.get("analysis"));
    }

    @Test
    @DisplayName("未指定版本时对比最新两个版本")
    void execute_NoVersionSpecified_ComparesLatestTwo() {
        VersionInfo v1 = createVersion(1L, "v1.0");
        VersionInfo v2 = createVersion(2L, "v2.0");
        VersionInfo v3 = createVersion(3L, "v3.0");
        when(versionInfoService.listVersions(10L)).thenReturn(List.of(v1, v2, v3));

        VersionDiffResponse diffResponse = new VersionDiffResponse();
        diffResponse.setSummary("diff content");
        when(versionInfoService.compareVersions(eq(10L), eq(2L), eq(1L))).thenReturn(diffResponse);

        VersionAnalyzeResponse analyzeResponse = new VersionAnalyzeResponse();
        analyzeResponse.setSummary("需要关注");
        when(versionInfoService.analyzeVersions(eq(10L), any(VersionAnalyzeRequest.class))).thenReturn(analyzeResponse);

        Map<String, Object> params = Map.of("snippetId", 10L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("snippetId 从上下文 search_top_id 获取")
    void execute_SnippetIdFromContext() {
        VersionInfo v1 = createVersion(1L, "v1");
        VersionInfo v2 = createVersion(2L, "v2");
        when(versionInfoService.listVersions(99L)).thenReturn(List.of(v1, v2));

        VersionDiffResponse diffResponse = new VersionDiffResponse();
        diffResponse.setSummary("diff");
        when(versionInfoService.compareVersions(eq(99L), eq(2L), eq(1L))).thenReturn(diffResponse);

        VersionAnalyzeResponse analyzeResponse = new VersionAnalyzeResponse();
        analyzeResponse.setSummary("ok");
        when(versionInfoService.analyzeVersions(eq(99L), any(VersionAnalyzeRequest.class))).thenReturn(analyzeResponse);

        Map<String, Object> params = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        context.put("search_top_id", 99L);

        SkillResult result = skill.execute(params, context);

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("缺少 snippetId 且上下文没有返回失败")
    void execute_NoSnippetId_ReturnsFailure() {
        SkillResult result = skill.execute(new HashMap<>(), new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("snippetId"));
    }

    @Test
    @DisplayName("版本数量不足 2 个返回失败")
    void execute_LessThanTwoVersions_ReturnsFailure() {
        VersionInfo v1 = createVersion(1L, "v1.0");
        when(versionInfoService.listVersions(5L)).thenReturn(List.of(v1));

        Map<String, Object> params = Map.of("snippetId", 5L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("不足 2 个"));
    }

    @Test
    @DisplayName("写入上下文 compare_* 键")
    void execute_WritesContext() {
        VersionInfo v1 = createVersion(1L, "v1.0");
        VersionInfo v2 = createVersion(2L, "v2.0");
        when(versionInfoService.listVersions(5L)).thenReturn(List.of(v1, v2));

        VersionDiffResponse diffResponse = new VersionDiffResponse();
        diffResponse.setSummary("diff");
        when(versionInfoService.compareVersions(eq(5L), eq(2L), eq(1L))).thenReturn(diffResponse);

        VersionAnalyzeResponse analyzeResponse = new VersionAnalyzeResponse();
        analyzeResponse.setSummary("分析结果");
        when(versionInfoService.analyzeVersions(eq(5L), any(VersionAnalyzeRequest.class))).thenReturn(analyzeResponse);

        Map<String, Object> params = Map.of("snippetId", 5L);
        Map<String, Object> context = new HashMap<>();

        skill.execute(params, context);

        assertNotNull(context.get("compare_versionA"));
        assertNotNull(context.get("compare_versionB"));
        assertEquals(diffResponse, context.get("compare_diff"));
        assertEquals(analyzeResponse, context.get("compare_analysis"));
    }

    @Test
    @DisplayName("异常时捕获并返回失败")
    void execute_Exception_ReturnsFailure() {
        when(versionInfoService.listVersions(1L)).thenThrow(new RuntimeException("数据库连接失败"));

        Map<String, Object> params = Map.of("snippetId", 1L);

        SkillResult result = skill.execute(params, new HashMap<>());

        assertFalse(result.getSuccess());
        assertTrue(result.getError().contains("数据库连接失败"));
    }

    private VersionInfo createVersion(Long id, String name) {
        VersionInfo v = new VersionInfo();
        v.setId(id);
        v.setVersionName(name);
        v.setDescription("测试版本");
        return v;
    }
}
