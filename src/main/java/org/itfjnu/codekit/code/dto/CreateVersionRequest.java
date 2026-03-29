package org.itfjnu.codekit.code.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "版本管理请求体")
public class CreateVersionRequest {
    private String versionName;
    private String description;
}
