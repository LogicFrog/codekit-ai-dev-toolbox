package org.itfjnu.codekit.system.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.common.config.CodeKitProperties;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.system.dto.FsItem;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/fs")
@RequiredArgsConstructor
@Tag(name = "文件系统接口")
public class FileSystemController {

    private final CodeKitProperties codeKitProperties;

    @GetMapping("/list")
    public ApiResponse<List<FsItem>> listFiles(@RequestParam(required = false) String path) {
        String workspaceRoot = codeKitProperties.getFs().getWorkspaceRoot();
        
        if (workspaceRoot == null || workspaceRoot.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFIG_ERROR, "工作区根目录未配置，请设置 codekit.fs.workspace-root");
        }

        String searchPath;
        if (path == null || path.isEmpty()) {
            searchPath = workspaceRoot;
        } else {
            searchPath = path;
        }

        File directory = new File(searchPath);

        if (!directory.exists()) {
            throw new BusinessException(ErrorCode.DIRECTORY_NOT_FOUND, "路径不存在: " + searchPath);
        }

        if (!directory.isDirectory()) {
            throw new BusinessException(ErrorCode.DIRECTORY_NOT_FOUND, "该路径不是目录: " + searchPath);
        }

        Path normalizedSearchPath;
        Path normalizedWorkspaceRoot;
        try {
            normalizedSearchPath = directory.getCanonicalFile().toPath().normalize();
            normalizedWorkspaceRoot = new File(workspaceRoot).getCanonicalFile().toPath().normalize();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "路径解析失败: " + e.getMessage());
        }

        if (!normalizedSearchPath.startsWith(normalizedWorkspaceRoot)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "非法路径：只能访问工作区目录内的文件");
        }

        File[] files = directory.listFiles();
        List<FsItem> items = new ArrayList<>();

        if (!normalizedSearchPath.equals(normalizedWorkspaceRoot) && directory.getParent() != null) {
            String parentPath = directory.getParent();
            try {
                Path normalizedParent = new File(parentPath).getCanonicalFile().toPath().normalize();
                if (normalizedParent.startsWith(normalizedWorkspaceRoot)) {
                    items.add(new FsItem(".. (返回上一级)", parentPath, true));
                }
            } catch (IOException ignored) {
            }
        }

        if (files != null) {
            for (File f : files) {
                if (f.isHidden()) continue;
                items.add(new FsItem(f.getName(), f.getAbsolutePath(), f.isDirectory()));
            }
        }

        items.sort((a, b) -> b.getIsDirectory().compareTo(a.getIsDirectory()));

        return ApiResponse.success(items);
    }
}
