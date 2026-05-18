package org.itfjnu.codekit.code.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VersionDiffResponse {
    private Long snippetId;
    private Long fromVersionId;
    private Long toVersionId;
    private Integer addedLines;
    private Integer removedLines;
    private Integer modifiedBlocks;
    private Double changeRate;
    private String summary;
    private List<VersionChangeBlock> blocks = new ArrayList<>();
}
