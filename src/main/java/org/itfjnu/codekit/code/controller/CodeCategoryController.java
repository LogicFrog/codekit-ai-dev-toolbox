package org.itfjnu.codekit.code.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.model.CodeCategory;
import org.itfjnu.codekit.code.service.CodeCategoryService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/code/categories")
@RequiredArgsConstructor
@Tag(name = "代码分类模块", description = "负责代码分类的增删改查")
public class CodeCategoryController {

    private final CodeCategoryService codeCategoryService;

    @Operation(summary = "查询分类列表", description = "获取所有代码分类")
    @GetMapping
    public ApiResponse<List<CodeCategory>> listCategories() {
        return ApiResponse.success(codeCategoryService.listCategories());
    }

    @Operation(summary = "创建分类", description = "创建新的代码分类")
    @PostMapping
    public ApiResponse<CodeCategory> createCategory(@RequestParam String categoryName) {
        return ApiResponse.success(codeCategoryService.createCategory(categoryName));
    }

    @Operation(summary = "重命名分类", description = "修改分类名称")
    @PutMapping("/{categoryId}")
    public ApiResponse<CodeCategory> renameCategory(@PathVariable Long categoryId,
                                                    @RequestParam String categoryName) {
        return ApiResponse.success(codeCategoryService.renameCategory(categoryId, categoryName));
    }

    @Operation(summary = "删除分类", description = "删除分类并将关联代码归入未分类")
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Boolean> deleteCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(codeCategoryService.deleteCategory(categoryId));
    }
}
