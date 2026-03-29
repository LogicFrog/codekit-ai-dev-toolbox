package org.itfjnu.codekit.code.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.CreateVersionRequest;
import org.itfjnu.codekit.code.dto.ScanRequest;
import org.itfjnu.codekit.code.dto.ScanStatusDTO;
import org.itfjnu.codekit.code.filesystem.LocalFileScanService;
import org.itfjnu.codekit.code.model.CodeDependency;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.repository.VersionInfoRepository;
import org.itfjnu.codekit.code.service.CodeManagerService;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码管理模块API接口
 */
@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
@Tag(name = "代码管理模块", description = "负责代码片段的扫描、导入、查询和管理")
public class CodeManagerController {

    private final CodeManagerService codeManagerService;
    private final LocalFileScanService localFileScanService;
    private final VersionInfoRepository versionInfoRepository;

    /**
     * 本地代码目录扫描
     * 请求方式: POST
     * 请求路径: /api/code/scan
     * 请求体: {"scanDir":"/Users/xxx/Documents/your-code-dir"}
     */
    @Operation(summary = "扫描本地代码目录", description = "递归扫描指定目录下的所有代码文件并入库")
    @PostMapping("/scan")
    public ApiResponse<Boolean> scanLocalCode(@Valid @RequestBody ScanRequest request) {
        String scanDir = request.getScanDir();
        return ApiResponse.success(localFileScanService.scanLocalCodeDir(scanDir));
    }

    @Operation(summary = "查询目录扫描状态", description = "异步任务状态查询：IDLE/RUNNING/COMPLETED/FAILED")
    @GetMapping("/scan/status")
    public ApiResponse<ScanStatusDTO> getScanStatus(@RequestParam String scanDir) {
        ScanStatusDTO status = localFileScanService.getScanStatus(scanDir);
        return ApiResponse.success(status);
    }

    @Operation(summary = "按路径导入单个文件", description = "根据文件路径读取代码内容并入库")
    @PostMapping("/save-by-path")
    public ApiResponse<CodeSnippet> saveCodeSnippetByPath(@RequestParam String filePath,
                                                          @RequestParam(required = false) String languageType,
                                                          @RequestParam(required = false) String tag) {
        CodeSnippet snippet = localFileScanService.importSingleFile(filePath, languageType, tag);
        if (snippet == null) {
            return ApiResponse.fail(ErrorCode.CODE_SAVE_FAILED, "保存失败");
        }
        return ApiResponse.success(snippet);
    }
    /**
     * 新增或更新代码片段
     * 请求方式: POST
     * 请求路径: /api/code/save
     */
    @Operation(summary = "新增或更新代码片段", description = "手动保存代码片段内容")
    @PostMapping("/save")
    public ApiResponse<CodeSnippet> saveCodeSnippet(@RequestBody CodeSnippet codeSnippet) {
        return ApiResponse.success(codeManagerService.saveOrUpdateCodeSnippet(codeSnippet));
    }

    /**
     * 根据 ID 删除代码片段
     * 请求方式: DELETE
     * 请求路径: /api/code/delete/{id}
     */
    @Operation(summary = "删除代码片段", description = "根据 ID 删除代码片段及其依赖")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deleteCodeSnippet(@PathVariable Long id) {
        return ApiResponse.success(codeManagerService.deleteCodeSnippetById(id));
    }

    /**
     * 根据 ID 查询代码片段详情
     * 请求方式: GET
     * 请求路径: /api/code/get/{id}
     */
    @Operation(summary = "查询代码片段详情", description = "根据 ID 获取代码片段完整信息")
    @GetMapping("/get/{id}")
    public ApiResponse<CodeSnippet> getCodeSnippet(@PathVariable Long id) {
        CodeSnippet snippet = codeManagerService.getCodeSnippetById(id);
        if (snippet == null) {
            return ApiResponse.fail(ErrorCode.CODE_NOT_FOUND);
        }
        return ApiResponse.success(snippet);
    }

    /**
     * 按标签模糊查询代码片段
     * 请求方式: GET
     * 请求路径: /api/code/tag
     * 请求参数: tag 示例: /api/code/tag?tag=Redis
     */
    @Operation(summary = "按标签查询", description = "根据标签模糊查询代码片段")
    @GetMapping("/tag")
    public ApiResponse<List<CodeSnippet>> listCodeByTag(@RequestParam String tag) {
        return ApiResponse.success(codeManagerService.listCodeSnippetByTag(tag));
    }

    /**
     * 按语言类型查询代码片段
     * 请求方式: GET
     * 请求路径: /api/code/language
     * 请求参数: type 示例: /api/code/language?type=Java
     */
    @Operation(summary = "按语言查询", description = "根据语言类型查询代码片段")
    @GetMapping("/language")
    public ApiResponse<List<CodeSnippet>> listCodeByLanguage(@RequestParam String type) {
        return ApiResponse.success(codeManagerService.listCodeSnippetByLanguage(type));
    }

    /**
     * 分页查询代码片段列表
     * 请求方式: GET
     * 请求路径: /api/code/page
     * 请求参数: page, size 等分页参数
     */
    @Operation(summary = "分页查询代码片段", description = "分页获取所有代码片段列表")
    @GetMapping("/page")
    public ApiResponse<Page<CodeSnippet>> listCodeByPage(Pageable pageable) {
        Page<CodeSnippet> page = codeManagerService.listCodeSnippetByPage(pageable);
        return ApiResponse.success(page);
    }

    /**
     * 查询指定代码片段的所有依赖
     * 请求方式: GET
     * 请求路径: /api/code/{id}/dependencies
     */
    @Operation(summary = "查询代码依赖", description = "获取指定代码片段的所有依赖关系")
    @GetMapping("/{id}/dependencies")
    public ApiResponse<List<CodeDependency>> listCodeDependencies(@PathVariable Long id) {
        return ApiResponse.success(codeManagerService.listDependenciesBySnippetId(id));
    }

    /**
     * 为代码片段创建版本快照
     * POST /api/code/{id}/create-version
     */
    @Operation(summary = "创建代码版本", description = "更新指定代码")
    @PostMapping("/{id}/create-version")
    public ApiResponse<VersionInfo> createVersion(
            @PathVariable Long id,
            @RequestBody CreateVersionRequest request
    ) {
        try {
            // 1. 查询当前代码片段
            CodeSnippet snippet = codeManagerService.getCodeSnippetById(id);
            if (snippet == null) {
                return ApiResponse.fail(ErrorCode.CODE_NOT_FOUND, "代码片段不存在");
            }

            // 2. 创建版本快照对象
            VersionInfo version = new VersionInfo();
            version.setSnippetId(id);
            version.setVersionName(request.getVersionName());
            version.setCodeContent(snippet.getCodeContent());  // 复制当前内容
            version.setCreateTime(LocalDateTime.now());
            version.setDescription(request.getDescription());

            // 3. 保存到 version_info 表
            VersionInfo savedVersion = versionInfoRepository.save(version);

            return ApiResponse.success(savedVersion);
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.VERSION_CREATE_FAILED, "创建版本失败：" + e.getMessage());
        }
    }

    /**
     * 查询代码片段的所有版本
     * GET /api/code/{id}/versions
     */
    @Operation(summary = "查询代码片段版本", description = "获取指定代码片段的所有版本")
    @GetMapping("/{id}/versions")
    public ApiResponse<List<VersionInfo>> listVersions(@PathVariable Long id) {
        try {
            // 1. 先检查代码片段是否存在
            CodeSnippet snippet = codeManagerService.getCodeSnippetById(id);
            if (snippet == null) {
                return ApiResponse.fail(ErrorCode.CODE_NOT_FOUND, "代码片段不存在");
            }

            // 2. 查询该代码片段的所有版本（按创建时间倒序）
            List<VersionInfo> versions = versionInfoRepository.findBySnippetIdOrderByCreateTimeDesc(id);

            return ApiResponse.success(versions);
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.VERSION_NOT_FOUND, "查询版本失败：" + e.getMessage());
        }
    }

}
