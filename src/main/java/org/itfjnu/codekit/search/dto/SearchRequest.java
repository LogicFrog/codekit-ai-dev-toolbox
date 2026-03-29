package org.itfjnu.codekit.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "检索请求参数")
public class SearchRequest {
    @Schema(description = "检索关键词（可为空，为空时仅按语言和标签筛选）", example = "Redis 连接")
    private String keyword;

    @Schema(description = "检索类型：keyword(关键词检索)/semantic(语义检索)", example = "keyword")
    private String searchType = "keyword";

    @Schema(description = "语言类型过滤", example = "Java")
    private String languageType;

    @Schema(description = "标签过滤", example = "Redis")
    private String tag;

    @Schema(description = "是否精确匹配", example = "false")
    private Boolean exactMatch = false;

    @Schema(description = "分页页码", example = "0")
    private Integer page = 0;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}
