package org.itfjnu.codekit.code.filesystem.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JavaScriptCodeMetadataParser extends ExternalScriptCodeMetadataParser {

    public JavaScriptCodeMetadataParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean supports(String languageType) {
        return "JavaScript/TypeScript".equalsIgnoreCase(languageType);
    }

    @Override
    protected String command() {
        return "node";
    }

    @Override
    protected String scriptPath() {
        return "src/main/resources/scripts/js_parser.js";
    }

    @Override
    protected String parserName() {
        return "JS";
    }
}
