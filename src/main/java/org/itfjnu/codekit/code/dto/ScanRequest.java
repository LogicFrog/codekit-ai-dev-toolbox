package org.itfjnu.codekit.code.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 本地代码扫描请求体
 */
@Data
@Schema(description = "扫描文件请求参数")
public class ScanRequest {

    @NotBlank
    private String scanDir;
}
