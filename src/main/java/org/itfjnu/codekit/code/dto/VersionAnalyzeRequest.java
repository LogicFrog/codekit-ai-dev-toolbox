package org.itfjnu.codekit.code.dto;

import lombok.Data;

@Data
public class VersionAnalyzeRequest {
    private Long fromVersionId;
    private Long toVersionId;

    // 可选：比如“只关注性能/安全/可维护性”
    private String focus;
}
