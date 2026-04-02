package org.itfjnu.codekit.code.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.CreateVersionRequest;
import org.itfjnu.codekit.code.model.CodeDependency;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
@Tag(name = "代码片段模块", description = "负责代码片段、依赖和版本入口")
public class CodeSnippetController {

    private final CodeSnippetService codeSnippetService;
    private final VersionInfoService versionInfoService;

    @Operation(summary = "新增或更新代码片段", description = "手动保存代码片段内容")
    @PostMapping("/save")
    public ApiResponse<CodeSnippet> saveCodeSnippet(@RequestBody CodeSnippet codeSnippet) {
        return ApiResponse.success(codeSnippetService.saveOrUpdateCodeSnippet(codeSnippet));
    }

    @Operation(summary = "删除代码片段", description = "根据 ID 删除代码片段及其依赖")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteCodeSnippet(@PathVariable Long id) {
        return ApiResponse.success(codeSnippetService.deleteCodeSnippetById(id));
    }

    @Operation(summary = "查询代码片段详情", description = "根据 ID 获取代码片段完整信息")
    @GetMapping("/get/{id}")
    public ApiResponse<CodeSnippet> getCodeSnippet(@PathVariable Long id) {
        CodeSnippet snippet = codeSnippetService.getCodeSnippetById(id);
        if (snippet == null) {
            throw new BusinessException(ErrorCode.CODE_NOT_FOUND);
        }
        return ApiResponse.success(snippet);
    }

    @Operation(summary = "按标签查询", description = "根据标签模糊查询代码片段")
    @GetMapping("/tag")
    public ApiResponse<List<CodeSnippet>> listCodeByTag(@RequestParam String tag) {
        return ApiResponse.success(codeSnippetService.listCodeSnippetByTag(tag));
    }

    @Operation(summary = "按语言查询", description = "根据语言类型查询代码片段")
    @GetMapping("/language")
    public ApiResponse<List<CodeSnippet>> listCodeByLanguage(@RequestParam String type) {
        return ApiResponse.success(codeSnippetService.listCodeSnippetByLanguage(type));
    }

    @Operation(summary = "按分类查询", description = "根据分类ID查询代码片段")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<CodeSnippet>> listCodeByCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(codeSnippetService.listCodeSnippetByCategory(categoryId));
    }

    @Operation(summary = "分页查询代码片段", description = "分页获取所有代码片段列表")
    @GetMapping("/page")
    public ApiResponse<Page<CodeSnippet>> listCodeByPage(Pageable pageable,
                                                         @RequestParam(required = false) Long categoryId) {
        Page<CodeSnippet> page = categoryId == null
                ? codeSnippetService.listCodeSnippetByPage(pageable)
                : codeSnippetService.listCodeSnippetByCategory(categoryId, pageable);
        return ApiResponse.success(page);
    }

    @Operation(summary = "设置代码分类", description = "为指定代码片段设置分类")
    @PutMapping("/{id}/category")
    public ApiResponse<CodeSnippet> assignCategory(@PathVariable Long id, @RequestParam Long categoryId) {
        return ApiResponse.success(codeSnippetService.assignCategory(id, categoryId));
    }

    @Operation(summary = "查询代码依赖", description = "获取指定代码片段的所有依赖关系")
    @GetMapping("/{id}/dependencies")
    public ApiResponse<List<CodeDependency>> listCodeDependencies(@PathVariable Long id) {
        return ApiResponse.success(codeSnippetService.listDependenciesBySnippetId(id));
    }

    @Operation(summary = "创建代码版本", description = "更新指定代码")
    @PostMapping("/{id}/create-version")
    public ApiResponse<VersionInfo> createVersion(@PathVariable Long id,
                                                  @RequestBody CreateVersionRequest request) {
        return ApiResponse.success(versionInfoService.createVersion(id, request));
    }

    @Operation(summary = "查询代码片段版本", description = "获取指定代码片段的所有版本")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<VersionInfo>> listVersions(@PathVariable Long id) {
        return ApiResponse.success(versionInfoService.listVersions(id));
    }
}
