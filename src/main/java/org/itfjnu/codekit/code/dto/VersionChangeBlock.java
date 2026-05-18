package org.itfjnu.codekit.code.dto;

import lombok.Data;

@Data
public class VersionChangeBlock {
    // ADD / REMOVE / MODIFY
    private String type;
    private Integer oldStartLine;
    private Integer oldEndLine;
    private Integer newStartLine;
    private Integer newEndLine;
    private String oldSnippet;
    private String newSnippet;
}
