package org.itfjnu.codekit.ai.agent.service;

import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;

public interface AgentOrchestratorService {

    AgentExecuteResponse execute(String instruction);
}
