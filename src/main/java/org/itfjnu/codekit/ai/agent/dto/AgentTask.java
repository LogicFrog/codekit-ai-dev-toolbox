package org.itfjnu.codekit.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    private String taskName;
    private String skillName;
    private Map<String, Object> params;

}
