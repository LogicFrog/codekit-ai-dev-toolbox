package org.itfjnu.codekit.code.filesystem.parser;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
public class CodeParseResult {
    String packageName;
    String className;
    @Builder.Default
    Set<String> tags = Set.of();
    @Builder.Default
    List<String> imports = List.of();
}
