package org.itfjnu.codekit.ai.agent.skill;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {

    @Test
    @DisplayName("查找存在的 Skill 返回 Optional 包含该 Skill")
    void findByName_ExistingSkill_ReturnsPresent() {
        Skill skill = new TestSkill("code_search");
        SkillRegistry registry = new SkillRegistry(List.of(skill));

        Optional<Skill> found = registry.findByName("code_search");

        assertTrue(found.isPresent());
        assertEquals("code_search", found.get().name());
    }

    @Test
    @DisplayName("查找不存在的 Skill 返回 Optional.empty()")
    void findByName_NonExistingSkill_ReturnsEmpty() {
        Skill skill = new TestSkill("code_search");
        SkillRegistry registry = new SkillRegistry(List.of(skill));

        Optional<Skill> found = registry.findByName("nonexistent");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("注册多个 Skill 时能正确查找每一个")
    void findByName_MultipleSkills_FindsCorrectly() {
        Skill s1 = new TestSkill("code_search");
        Skill s2 = new TestSkill("ai_explain");
        Skill s3 = new TestSkill("version_list");
        SkillRegistry registry = new SkillRegistry(List.of(s1, s2, s3));

        assertTrue(registry.findByName("code_search").isPresent());
        assertTrue(registry.findByName("ai_explain").isPresent());
        assertTrue(registry.findByName("version_list").isPresent());
    }

    @Test
    @DisplayName("空 Skill 列表不会抛出异常")
    void findByName_EmptyList_ReturnsEmpty() {
        SkillRegistry registry = new SkillRegistry(List.of());

        Optional<Skill> found = registry.findByName("anything");

        assertTrue(found.isEmpty());
    }

    private static final class TestSkill implements Skill {
        private final String name;
        TestSkill(String name) { this.name = name; }

        @Override public String name() { return name; }
        @Override public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
            return SkillResult.builder().success(true).skillName(name).build();
        }
    }
}
