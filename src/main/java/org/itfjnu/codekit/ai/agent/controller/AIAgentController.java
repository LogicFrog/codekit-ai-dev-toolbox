package org.itfjnu.codekit.ai.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteRequest;
import org.itfjnu.codekit.ai.agent.dto.AgentExecuteResponse;
import org.itfjnu.codekit.ai.agent.service.AgentOrchestratorService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
@Tag(name = "AI Agent", description = "任务拆解 + Skill 调度")
public class AIAgentController {
    private final AgentOrchestratorService agentOrchestratorService;

    private static final long AGENT_TIMEOUT_MS = 300000L;

    @Operation(summary = "执行 Agent 指令")
    @PostMapping("/execute")
    public DeferredResult<ApiResponse<AgentExecuteResponse>> execute(@RequestBody AgentExecuteRequest request) {
        DeferredResult<ApiResponse<AgentExecuteResponse>> deferredResult = new DeferredResult<>(AGENT_TIMEOUT_MS);
        Thread.startVirtualThread(() -> {
            try {
                AgentExecuteResponse response = agentOrchestratorService.execute(request.getInstruction());
                deferredResult.setResult(ApiResponse.success(response));
            } catch (Exception e) {
                log.error("Agent 执行失败", e);
                deferredResult.setResult(ApiResponse.fail("Agent 执行失败: " + e.getMessage()));
            }
        });
        deferredResult.onTimeout(() ->
                log.warn("Agent 执行超时 ({}ms), instruction: {}", AGENT_TIMEOUT_MS,
                        request.getInstruction() != null ? request.getInstruction().substring(0, Math.min(80, request.getInstruction().length())) : ""));
        return deferredResult;
    }

    @Operation(summary = "执行 Agent 指令（SSE 流式）")
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(@RequestBody AgentExecuteRequest request) {
        return agentOrchestratorService.executeStream(request.getInstruction());
    }

    /**
     *   入参DTO AgentExecuteRequest
     *   private String instruction; // 用户自然语言指令
     *   private String sessionId;   // 可选 会话ID
     *
     */
}
