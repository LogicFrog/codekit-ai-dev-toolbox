package org.itfjnu.codekit.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI模块", description = "提供代码解释、智能对话等AI辅助功能")
public class AIController {

    private final AIService aiService;

    @Operation(summary = "AI对话", description = "与AI进行自由对话")
    @PostMapping("/chat")
    public ApiResponse<AIChatResponse> chat(@RequestBody AIChatRequest request) {
        AIChatResponse response = aiService.chat(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "代码解释", description = "让AI解释选中的代码")
    @PostMapping("/explain")
    public ApiResponse<AIChatResponse> explain(@RequestBody AIChatRequest request) {
        AIChatResponse response = aiService.explain(request);
        return ApiResponse.success(response);
    }
}
