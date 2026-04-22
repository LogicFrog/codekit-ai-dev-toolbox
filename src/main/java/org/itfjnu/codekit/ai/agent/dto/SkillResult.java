package org.itfjnu.codekit.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResult {

    private Boolean success;
    private String skillName;
    private Object data;
    private String error;

}
