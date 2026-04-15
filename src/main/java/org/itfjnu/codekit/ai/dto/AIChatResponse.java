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
@Schema(description = "AI聊天响应")
public class AIChatResponse {

    @Schema(description = "回答内容")
    private String answer;

    @Schema(description = "建议列表")
    private List<String> suggestions;

    @Schema(description = "代码块列表")
    private List<CodeBlock> codeBlocks;

    @Schema(description = "错误信息")
    private String error;

    @Schema(description = "会话ID")
    private String sessionId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeBlock {
        @Schema(description = "代码语言")
        private String language;

        @Schema(description = "代码内容")
        private String code;

        @Schema(description = "代码说明")
        private String description;

    }
}
