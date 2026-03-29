package org.itfjnu.codekit.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 检索响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "检索响应结果")
public class SearchResponse {
    @Schema(description = "代码片段ID")
    private Long id;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "代码内容（预览）")
    private String codePreview;

    @Schema(description = "语言类型")
    private String languageType;

    @Schema(description = "类名")
    private String className;

    @Schema(description = "包名")
    private String packageName;

    @Schema(description = "标签列表")
    private java.util.Set<String> tags;

    @Schema(description = "匹配度（0-1）")
    private Double relevanceScore;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "高亮片段（匹配的关键词）")
    private String highlight;
}
