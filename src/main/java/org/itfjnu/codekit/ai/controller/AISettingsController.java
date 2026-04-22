package org.itfjnu.codekit.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.ai.service.AISettingsService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/settings")
@RequiredArgsConstructor
@Tag(name = "AI设置", description = "AI 运行参数设置")
public class AISettingsController {

    private final AISettingsService aiSettingsService;

    @Operation(summary = "获取温度值", description = "获取当前 AI temperature")
    @GetMapping("/temperature")
    public ApiResponse<Double> getTemperature() {
        return ApiResponse.success(aiSettingsService.getTemperature());
    }

    @Operation(summary = "设置温度值", description = "设置当前 AI temperature，范围 0.0~2.0")
    @PutMapping("/temperature")
    public ApiResponse<Double> setTemperature(@RequestParam Double value) {
        if (value == null || value < 0.0 || value > 2.0) {
            throw new IllegalArgumentException("temperature 必须在 0.0 到 2.0 之间");
        }
        return ApiResponse.success(aiSettingsService.setTemperature(value));
    }
}
