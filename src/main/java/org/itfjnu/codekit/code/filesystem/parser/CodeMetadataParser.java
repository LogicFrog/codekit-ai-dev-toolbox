package org.itfjnu.codekit.code.filesystem.parser;

import org.itfjnu.codekit.code.model.CodeSnippet;

public interface CodeMetadataParser {

    boolean supports(String languageType);

    CodeParseResult parse(CodeSnippet snippet);
}
