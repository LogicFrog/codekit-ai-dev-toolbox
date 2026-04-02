package org.itfjnu.codekit.code.filesystem.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.ServiceException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public abstract class ExternalScriptCodeMetadataParser implements CodeMetadataParser {

    private final ObjectMapper objectMapper;

    @Override
    public CodeParseResult parse(CodeSnippet snippet) {
        if (snippet.getCodeContent() == null || snippet.getCodeContent().isBlank()) {
            return CodeParseResult.builder().build();
        }

        try {
            String result = runExternalParser(command(), scriptPath(), snippet.getCodeContent());
            return parseResult(result);
        } catch (Exception e) {
            log.warn("【{} 解析失败】文件 {}: {}", parserName(), snippet.getFilePath(), e.getMessage());
            return CodeParseResult.builder().build();
        }
    }

    protected abstract String command();

    protected abstract String scriptPath();

    protected abstract String parserName();

    private String runExternalParser(String command, String scriptPath, String code) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command, scriptPath);
        Process process = processBuilder.start();

        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.write(code.getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new ServiceException(ErrorCode.CODE_PARSE_FAILED, "代码解析失败，退出码: " + exitCode);
        }
        return output.toString();
    }

    private CodeParseResult parseResult(String jsonResult) {
        try {
            JsonNode root = objectMapper.readTree(jsonResult);
            if (root.has("error")) {
                log.warn("解析脚本返回错误: {}", root.get("error").asText());
                return CodeParseResult.builder().build();
            }

            Set<String> tags = new HashSet<>();
            List<String> imports = new ArrayList<>();

            if (root.has("functions")) {
                root.get("functions").forEach(function -> {
                    String functionName = function.asText();
                    if (functionName.startsWith("innerClass:") || functionName.startsWith("method:")) {
                        tags.add(functionName);
                    } else {
                        tags.add("function:" + functionName);
                    }
                });
            }

            if (root.has("imports")) {
                root.get("imports").forEach(importNode -> imports.add(importNode.asText()));
            }

            return CodeParseResult.builder()
                    .className(readNullableText(root, "className"))
                    .tags(tags)
                    .imports(List.copyOf(imports))
                    .build();
        } catch (Exception e) {
            log.error("解析 JSON 结果失败: {}", e.getMessage());
            return CodeParseResult.builder().build();
        }
    }

    private String readNullableText(JsonNode root, String fieldName) {
        if (!root.has(fieldName) || root.get(fieldName).isNull()) {
            return null;
        }
        return root.get(fieldName).asText();
    }
}
