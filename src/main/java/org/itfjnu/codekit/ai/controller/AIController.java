package org.itfjnu.codekit.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.model.ChatMessage;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.ai.service.SessionHistoryService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI模块", description = "提供代码解释、智能对话等AI辅助功能")
public class AIController {

    private final AIService aiService;
    private final SessionHistoryService sessionHistoryService;

    @Operation(summary = "AI对话", description = "与AI进行自由对话")
    @PostMapping("/chat")
    public ApiResponse<AIChatResponse> chat(@RequestBody AIChatRequest request) {
        AIChatResponse response = aiService.chat(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "AI对话流式返回", description = "与AI进行自由对话（SSE 流式）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AIChatRequest request) {
        return aiService.chatStream(request);
    }

    @Operation(summary = "代码解释", description = "让AI解释选中的代码")
    @PostMapping("/explain")
    public ApiResponse<AIChatResponse> explain(@RequestBody AIChatRequest request) {
        AIChatResponse response = aiService.explain(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "清空会话", description = "清空指定 sessionId 的会话历史")
    @DeleteMapping("/session/{sessionId}")
    public ApiResponse<Boolean> clearSession(@PathVariable String sessionId) {
        Boolean cleared = sessionHistoryService.clearSession(sessionId);
        return ApiResponse.success(cleared);
    }

    @Operation(summary = "查询会话历史", description = "按 sessionId 查询最近多轮对话历史")
    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "10") int maxRounds
    ) {
        List<ChatMessage> messages = sessionHistoryService.getRecentMessages(sessionId, maxRounds);
        return ApiResponse.success(messages);
    }
}
