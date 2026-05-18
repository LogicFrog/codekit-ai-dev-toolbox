package org.itfjnu.codekit.code.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VersionRollbackResponse {
    private Long snippetId;
    private Long rollbackToVersionId;
    private Long backupVersionId;
    private String backupVersionName;
    private LocalDateTime rollbackTime;
}
