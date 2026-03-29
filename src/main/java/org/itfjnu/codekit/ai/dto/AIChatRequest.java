package org.itfjnu.codekit.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI聊天请求")
public class AIChatRequest {

    @Schema(description = "问题", example = "这段代码是什么意思？")
    private String question;

    @Schema(description = "代码内容", example = "public class Hello {}")
    private String code;

    @Schema(description = "语言类型", example = "Java")
    private String languageType;

    @Schema(description = "会话ID，用于上下文关联", example = "session-123")
    private String sessionId;
}
