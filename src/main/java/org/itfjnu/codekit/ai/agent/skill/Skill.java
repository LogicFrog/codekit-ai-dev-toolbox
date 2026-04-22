package org.itfjnu.codekit.ai.agent.skill;

import org.itfjnu.codekit.ai.agent.dto.SkillResult;

import java.util.Map;

public interface Skill {

    /** Skill 唯一名 */
    String name();

    /**
     * @param params 当前任务参数
     * @param context 整个执行上下文（上一步结果会写到这里）
     */
    SkillResult execute(Map<String, Object> params, Map<String, Object> context);
}


