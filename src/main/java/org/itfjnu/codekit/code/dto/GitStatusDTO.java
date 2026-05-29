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
@Schema(description = "Git 仓库状态")
public class GitStatusDTO {

    @Schema(description = "是否已初始化 Git 仓库")
    private boolean initialized;

    @Schema(description = "仓库根目录路径")
    private String repoPath;

    @Schema(description = "当前分支名")
    private String currentBranch;

    @Schema(description = "已暂存文件数")
    private int stagedCount;

    @Schema(description = "已修改未暂存文件数")
    private int modifiedCount;

    @Schema(description = "未跟踪文件数")
    private int untrackedCount;

    @Schema(description = "修改的文件列表")
    private List<String> changedFiles;
}
