package org.itfjnu.codekit.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 设置 DTO")
public class AISettingsDTO {

    @Schema(description = "温度参数 (0.0~2.0)", example = "1.0")
    private Double temperature;

    @Schema(description = "AI 提供商代码", example = "doubao")
    private String provider;

    @Schema(description = "AI 模型名称", example = "doubao-seed-2-0-pro-260215")
    private String model;

    @Schema(description = "API 密钥（返回时脱敏，保存时传明文）", example = "97ab****c7")
    private String apiKey;

    @Schema(description = "自定义 API 地址（为空时使用提供商默认地址）")
    private String baseUrl;

    @Schema(description = "编辑器主题", example = "vs-dark")
    private String editorTheme;

    @Schema(description = "编辑器字体大小 (12~24)", example = "14")
    private Integer fontSize;

    @Schema(description = "是否自动保存", example = "false")
    private Boolean autoSave;

    @Schema(description = "默认分页大小", example = "20")
    private Integer pageSize;

    @Schema(description = "上下文最大 Token 数", example = "4096")
    private Integer maxContextTokens;

    @Schema(description = "上下文最大历史轮数", example = "4")
    private Integer contextWindowRounds;

    @Schema(description = "Embedding API 密钥（返回时脱敏，保存时传明文）")
    private String embeddingApiKey;
}
