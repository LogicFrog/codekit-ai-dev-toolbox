package org.itfjnu.codekit.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "LLM 提供商信息")
public class ProviderInfo {

    @Schema(description = "提供商代码", example = "doubao")
    private String code;

    @Schema(description = "提供商显示名称", example = "豆包 (Doubao)")
    private String displayName;

    @Schema(description = "默认 API 地址")
    private String defaultBaseUrl;

    @Schema(description = "推荐模型列表", example = "[\"doubao-seed-2-0-pro-260215\"]")
    private List<String> defaultModels;
}
