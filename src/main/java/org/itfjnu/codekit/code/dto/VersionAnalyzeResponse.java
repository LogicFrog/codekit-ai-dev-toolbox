package org.itfjnu.codekit.code.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VersionAnalyzeResponse {
    private Long snippetId;
    private Long fromVersionId;
    private Long toVersionId;
    private String summary;
    private String riskLevel;
    private List<String> risks = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<String> testFocus = new ArrayList<>();
    private String rawAnswer;
}
