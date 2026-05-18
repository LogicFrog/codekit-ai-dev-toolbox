package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.code.dto.VersionAnalyzeRequest;
import org.itfjnu.codekit.code.dto.VersionAnalyzeResponse;
import org.itfjnu.codekit.code.dto.VersionDiffResponse;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitCompareSkillImpl implements Skill {

    private final VersionInfoService versionInfoService;

    @Override
    public String name() {
        return "git_compare";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            // 获取 snippetId，优先从 params 拿，没有从上下文拿
            Object snippetIdObj = params.get("snippetId");
            if (snippetIdObj == null) {
                snippetIdObj = context.get("search_top_id");
            }
            if (snippetIdObj == null) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("缺少 snippetId，且上下文没有 search_top_id")
                        .build();
            }

            Long snippetId = Long.valueOf(String.valueOf(snippetIdObj));

            // 获取所有版本列表
            List<VersionInfo> allVersions = versionInfoService.listVersions(snippetId);
            if (allVersions.size() < 2) {
                return SkillResult.builder()
                        .success(Boolean.FALSE)
                        .skillName(name())
                        .error("版本不足 2 个，无法对比，请先创建更多版本")
                        .data(Map.of("versionCount", allVersions.size()))
                        .build();
            }

            // 获取两个版本 ID
            Object versionAObj = params.get("versionA");
            Object versionBObj = params.get("versionB");

            Long versionAId;
            Long versionBId;

            if (versionAObj != null && versionBObj != null) {
                versionAId = Long.valueOf(String.valueOf(versionAObj));
                versionBId = Long.valueOf(String.valueOf(versionBObj));
            } else {
                // 默认对比最新两个版本（索引 0 是最新的，索引 1 是次新的）
                versionAId = allVersions.get(1).getId(); // 较旧版本
                versionBId = allVersions.get(0).getId(); // 较新版本
            }

            // 1. 获取版本差异
            VersionDiffResponse diffResponse = versionInfoService.compareVersions(snippetId, versionAId, versionBId);

            // 2. AI 分析版本差异
            VersionAnalyzeRequest analyzeRequest = new VersionAnalyzeRequest();
            analyzeRequest.setFromVersionId(versionAId);
            analyzeRequest.setToVersionId(versionBId);
            analyzeRequest.setFocus("通用质量、潜在风险、测试建议");
            VersionAnalyzeResponse analyzeResponse = versionInfoService.analyzeVersions(snippetId, analyzeRequest);

            // 将结果写入上下文，供后续 Skill 使用
            context.put("compare_versionA", allVersions.stream().filter(v -> v.getId().equals(versionAId)).findFirst().orElse(null));
            context.put("compare_versionB", allVersions.stream().filter(v -> v.getId().equals(versionBId)).findFirst().orElse(null));
            context.put("compare_diff", diffResponse);
            context.put("compare_analysis", analyzeResponse);

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "snippetId", snippetId,
                            "versionAId", versionAId,
                            "versionBId", versionBId,
                            "diff", diffResponse,
                            "analysis", analyzeResponse
                    ))
                    .build();

        } catch (Exception e) {
            log.error("GitCompareSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }
}
