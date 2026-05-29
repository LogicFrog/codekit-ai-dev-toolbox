package org.itfjnu.codekit.code.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.GitCommitInfo;
import org.itfjnu.codekit.code.dto.GitStatusDTO;
import org.itfjnu.codekit.code.service.GitService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/code/git")
@RequiredArgsConstructor
@Tag(name = "Git 版本管理", description = "基于 JGit 的 Git 仓库操作")
public class GitController {

    private final GitService gitService;

    @Operation(summary = "初始化 Git 仓库", description = "在指定路径初始化 Git 仓库")
    @PostMapping("/init")
    public ApiResponse<Boolean> initRepository(@RequestParam String path) {
        return ApiResponse.success(gitService.initRepository(path));
    }

    @Operation(summary = "获取 Git 状态", description = "获取指定路径 Git 仓库的工作区状态")
    @GetMapping("/status")
    public ApiResponse<GitStatusDTO> getStatus(@RequestParam String path) {
        return ApiResponse.success(gitService.getStatus(path));
    }

    @Operation(summary = "提交变更", description = "提交当前工作区的所有变更")
    @PostMapping("/commit")
    public ApiResponse<String> commit(@RequestParam String path, @RequestParam String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("提交消息不能为空");
        }
        return ApiResponse.success(gitService.commit(path, message));
    }

    @Operation(summary = "获取提交历史", description = "获取 Git 仓库的提交历史")
    @GetMapping("/history")
    public ApiResponse<List<GitCommitInfo>> getHistory(@RequestParam String path,
                                                        @RequestParam(defaultValue = "20") int maxCount) {
        return ApiResponse.success(gitService.getHistory(path, maxCount));
    }

    @Operation(summary = "获取版本差异", description = "获取两个引用之间的代码差异")
    @GetMapping("/diff")
    public ApiResponse<String> diff(@RequestParam String path,
                                     @RequestParam(defaultValue = "HEAD~1") String oldRef,
                                     @RequestParam(defaultValue = "HEAD") String newRef) {
        return ApiResponse.success(gitService.diff(path, oldRef, newRef));
    }
}
