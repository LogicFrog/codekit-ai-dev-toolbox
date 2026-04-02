package org.itfjnu.codekit.code.filesystem.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PythonCodeMetadataParser extends ExternalScriptCodeMetadataParser {

    public PythonCodeMetadataParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean supports(String languageType) {
        return "Python".equalsIgnoreCase(languageType);
    }

    @Override
    protected String command() {
        return "python3";
    }

    @Override
    protected String scriptPath() {
        return "src/main/resources/scripts/python_parser.py";
    }

    @Override
    protected String parserName() {
        return "Python";
    }
}
