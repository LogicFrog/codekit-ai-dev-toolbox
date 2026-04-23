package org.itfjnu.codekit.ai.agent.skill.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.itfjnu.codekit.ai.agent.skill.Skill;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VersionListSkillImpl implements Skill {

    private final VersionInfoService versionInfoService;

    @Override
    public String name() {
        return "version_list";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        try {
            // 优先读 params.snippetId；没有就尝试用 code_search 的 top id
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
            List<VersionInfo> versions = versionInfoService.listVersions(snippetId);

            context.put("version_count", versions.size());
            context.put("version_items", versions);

            return SkillResult.builder()
                    .success(Boolean.TRUE)
                    .skillName(name())
                    .data(Map.of(
                            "snippetId", snippetId,
                            "count", versions.size(),
                            "items", versions
                    ))
                    .build();
        } catch (Exception e) {
            log.error("VersionListSkill 执行失败", e);
            return SkillResult.builder()
                    .success(Boolean.FALSE)
                    .skillName(name())
                    .error(e.getMessage())
                    .build();
        }
    }
}
