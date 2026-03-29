package org.itfjnu.codekit.system.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.system.dto.FsItem;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/fs")
@Tag(name = "文件系统接口")
public class FileSystemController {

    @GetMapping("/list")
    public ApiResponse<List<FsItem>> listFiles(@RequestParam(required = false) String path) {
        // 1. 确定搜索路径（为空则取家目录）
        String searchPath = (path == null || path.isEmpty()) ? System.getProperty("user.home") : path;
        File directory = new File(searchPath);

        if (!directory.exists() || !directory.isDirectory()) {
            throw new BusinessException(ErrorCode.DIRECTORY_NOT_FOUND, "路径不存在或不是有效目录: " + searchPath);
        }

        // 2. 获取并转换子项
        File[] files = directory.listFiles();
        List<FsItem> items = new ArrayList<>();

        // 先加一个“..”项，方便用户返回上一级
        if (directory.getParent() != null) {
            items.add(new FsItem(".. (返回上一级)", directory.getParent(), true));
        }

        if (files != null) {
            for (File f : files) {
                if (f.isHidden()) continue; // 过滤隐藏文件
                items.add(new FsItem(f.getName(), f.getAbsolutePath(), f.isDirectory()));
            }
        }

        // 建议：对列表进行排序（目录在前，文件在后）
        items.sort((a, b) -> b.getIsDirectory().compareTo(a.getIsDirectory()));

        return ApiResponse.success(items);
    }
}
