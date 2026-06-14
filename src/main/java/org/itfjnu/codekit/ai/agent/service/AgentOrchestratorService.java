package org.itfjnu.codekit.ai.agent.service;

import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentOrchestratorService {

    AgentExecuteResponse execute(String instruction);

    SseEmitter executeStream(String instruction);
}
