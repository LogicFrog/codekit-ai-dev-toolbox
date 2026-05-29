package org.itfjnu.codekit.code.dto;

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
@Schema(description = "Git 提交信息")
public class GitCommitInfo {

    @Schema(description = "提交哈希（短）")
    private String shortHash;

    @Schema(description = "提交哈希（完整）")
    private String fullHash;

    @Schema(description = "提交消息")
    private String message;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "提交时间")
    private String commitTime;

    @Schema(description = "变更文件列表")
    private List<String> changedFiles;
}
