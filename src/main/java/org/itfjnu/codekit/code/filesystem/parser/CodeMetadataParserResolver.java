package org.itfjnu.codekit.code.filesystem.parser;

import org.itfjnu.codekit.code.model.CodeSnippet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodeMetadataParserResolver {

    private final List<CodeMetadataParser> parsers;

    public CodeMetadataParserResolver(List<CodeMetadataParser> parsers) {
        this.parsers = parsers;
    }

    public CodeParseResult parse(CodeSnippet snippet) {
        if (snippet.getLanguageType() == null) {
            return CodeParseResult.builder().build();
        }

        return parsers.stream()
                .filter(parser -> parser.supports(snippet.getLanguageType()))
                .findFirst()
                .map(parser -> parser.parse(snippet))
                .orElseGet(() -> CodeParseResult.builder().build());
    }
}
