package org.itfjnu.codekit.ai.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteRequest;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.itfjnu.codekit.ai.agent.service.AgentOrchestratorService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
@Tag(name = "AI Agent", description = "任务拆解 + Skill 调度")
public class AIAgentController {
    private final AgentOrchestratorService agentOrchestratorService;

    @Operation(summary = "执行 Agent 指令")
    @PostMapping("/execute")
    public ApiResponse<AgentExecuteResponse> execute(@RequestBody AgentExecuteRequest request) {
        AgentExecuteResponse response = agentOrchestratorService.execute(request.getInstruction());
        return ApiResponse.success(response);
    }

}
