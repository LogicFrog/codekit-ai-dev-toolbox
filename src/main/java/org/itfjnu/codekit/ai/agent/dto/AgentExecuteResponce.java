package org.itfjnu.codekit.ai.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecuteResponce {
    private String instruction;
    private List<AgentTask> tasks;
    private List<SkillResult> results;
    private String summary;
}
