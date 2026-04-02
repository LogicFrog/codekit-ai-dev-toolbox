package org.itfjnu.codekit.code.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.ScanRequest;
import org.itfjnu.codekit.code.dto.ScanStatusDTO;
import org.itfjnu.codekit.code.filesystem.LocalFileScanService;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.common.dto.ApiResponse;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
@Tag(name = "代码扫描模块", description = "负责本地目录扫描和文件导入")
public class CodeScanController {

    private final LocalFileScanService localFileScanService;

    @Operation(summary = "扫描本地代码目录", description = "递归扫描指定目录下的所有代码文件并入库")
    @PostMapping("/scan")
    public ApiResponse<Boolean> scanLocalCode(@Valid @RequestBody ScanRequest request) {
        return ApiResponse.success(localFileScanService.scanLocalCodeDir(request.getScanDir()));
    }

    @Operation(summary = "查询目录扫描状态", description = "异步任务状态查询：IDLE/RUNNING/COMPLETED/FAILED")
    @GetMapping("/scan/status")
    public ApiResponse<ScanStatusDTO> getScanStatus(@RequestParam String scanDir) {
        return ApiResponse.success(localFileScanService.getScanStatus(scanDir));
    }

    @Operation(summary = "按路径导入单个文件", description = "根据文件路径读取代码内容并入库")
    @PostMapping("/save-by-path")
    public ApiResponse<CodeSnippet> saveCodeSnippetByPath(@RequestParam String filePath,
                                                          @RequestParam(required = false) String languageType,
                                                          @RequestParam(required = false) String tag,
                                                          @RequestParam(required = false) Long categoryId) {
        CodeSnippet snippet = localFileScanService.importSingleFile(filePath, languageType, tag, categoryId);
        if (snippet == null) {
            throw new ServiceException(ErrorCode.CODE_SAVE_FAILED, "保存失败");
        }
        return ApiResponse.success(snippet);
    }
}
