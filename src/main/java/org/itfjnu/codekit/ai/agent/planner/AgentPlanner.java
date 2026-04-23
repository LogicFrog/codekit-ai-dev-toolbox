package org.itfjnu.codekit.ai.agent.planner;

import org.itfjnu.codekit.ai.agent.dto.AgentTask;

import java.util.List;

public interface AgentPlanner {
    List<AgentTask> plan(String instruction);
}
