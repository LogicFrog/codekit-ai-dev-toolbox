package org.itfjnu.codekit.code.filesystem.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.filesystem.parser.CodeMetadataParserResolver;
import org.itfjnu.codekit.code.filesystem.parser.CodeParseResult;
import org.itfjnu.codekit.code.model.CodeCategory;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.service.CodeCategoryService;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeFileProcessor {

    private static final long MAX_FILE_SIZE = 1024 * 1024;

    private final CodeSnippetService codeSnippetService;
    private final CodeCategoryService codeCategoryService;
    private final CodeMetadataParserResolver parserResolver;
    private final ScanTaskTracker scanTaskTracker;

    public CodeSnippet processFile(File codeFile, String rootDir, String languageType, String tag, Long categoryId) {
        String filePath = codeFile.getAbsolutePath();

        if (rootDir != null) {
            scanTaskTracker.markProcessed(rootDir);
        }

        if (codeFile.length() > MAX_FILE_SIZE) {
            log.warn("【文件处理】文件过大，跳过读取：{} ({} KB)", filePath, codeFile.length() / 1024);
            markSkip(rootDir);
            return null;
        }

        try {
            String fileMd5 = calculateFileMd5(codeFile);
            if (fileMd5 == null) {
                markSkip(rootDir);
                return null;
            }

            CodeSnippet existingByPath = codeSnippetService.getCodeSnippetByPath(filePath);
            CodeSnippet existingByMd5 = codeSnippetService.getCodeSnippetByMd5(fileMd5);

            if (existingByPath != null) {
                return handleExistingPath(codeFile, existingByPath, fileMd5, rootDir);
            }

            if (existingByMd5 != null) {
                return handleMovedFile(codeFile, existingByMd5, rootDir);
            }

            return createNewSnippet(codeFile, fileMd5, languageType, tag, categoryId, rootDir);
        } catch (IOException e) {
            log.error("【文件处理】读取文件失败：{}，原因：{}", filePath, e.getMessage(), e);
            markFailed(rootDir);
            return null;
        }
    }

    private CodeSnippet handleExistingPath(File codeFile, CodeSnippet existingSnippet,
                                           String fileMd5, String rootDir) throws IOException {
        String filePath = codeFile.getAbsolutePath();
        if (fileMd5.equals(existingSnippet.getFileMd5())) {
            log.debug("【文件处理】文件内容未变，跳过：{}", filePath);
            markSkip(rootDir);
            return existingSnippet;
        }

        log.info("【文件处理】文件内容已更新：{}", filePath);
        existingSnippet.setCodeContent(Files.readString(codeFile.toPath()));
        existingSnippet.setFileMd5(fileMd5);

        CodeParseResult parseResult = parserResolver.parse(existingSnippet);
        applyParseResult(existingSnippet, parseResult);

        CodeSnippet savedSnippet = codeSnippetService.saveOrUpdateCodeSnippet(existingSnippet);
        saveDependencies(savedSnippet.getId(), parseResult.getImports());
        markSuccess(rootDir);
        return savedSnippet;
    }

    private CodeSnippet handleMovedFile(File codeFile, CodeSnippet existingSnippet, String rootDir) {
        log.info("【文件处理】检测到文件移动：{} -> {}", existingSnippet.getFilePath(), codeFile.getAbsolutePath());
        existingSnippet.setFilePath(codeFile.getAbsolutePath());
        existingSnippet.setFileName(codeFile.getName());

        CodeSnippet savedSnippet = codeSnippetService.saveOrUpdateCodeSnippet(existingSnippet);
        markSuccess(rootDir);
        return savedSnippet;
    }

    private CodeSnippet createNewSnippet(File codeFile, String fileMd5, String languageType,
                                         String tag, Long categoryId, String rootDir) throws IOException {
        log.info("【文件处理】新文件：{}", codeFile.getAbsolutePath());
        CodeSnippet snippet = new CodeSnippet();
        snippet.setFilePath(codeFile.getAbsolutePath());
        snippet.setFileName(codeFile.getName());
        snippet.setCodeContent(Files.readString(codeFile.toPath()));
        snippet.setLanguageType(resolveLanguageType(codeFile.getName(), languageType));
        snippet.setFileMd5(fileMd5);
        snippet.setTags(new HashSet<>());
        applyRequestedCategory(snippet, categoryId);

        applyRequestedTags(snippet, tag);

        CodeParseResult parseResult = parserResolver.parse(snippet);
        applyParseResult(snippet, parseResult);

        if (snippet.getTags().isEmpty()) {
            snippet.getTags().add("未分类");
        }

        CodeSnippet savedSnippet = codeSnippetService.saveOrUpdateCodeSnippet(snippet);
        saveDependencies(savedSnippet.getId(), parseResult.getImports());
        markSuccess(rootDir);
        return savedSnippet;
    }

    private void applyRequestedCategory(CodeSnippet snippet, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CodeCategory category = codeCategoryService.getCategoryById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在，ID: " + categoryId);
        }
        snippet.setCategory(category);
    }

    private void saveDependencies(Long snippetId, List<String> imports) {
        if (snippetId != null && imports != null && !imports.isEmpty()) {
            codeSnippetService.saveDependencies(snippetId, imports);
        }
    }

    private void applyRequestedTags(CodeSnippet snippet, String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        for (String tagItem : tag.split(",")) {
            String normalizedTag = tagItem.trim();
            if (!normalizedTag.isEmpty()) {
                snippet.getTags().add(normalizedTag);
            }
        }
    }

    private void applyParseResult(CodeSnippet snippet, CodeParseResult parseResult) {
        if (parseResult.getPackageName() != null) {
            snippet.setPackageName(parseResult.getPackageName());
        }
        if (parseResult.getClassName() != null) {
            snippet.setClassName(parseResult.getClassName());
        }
        if (parseResult.getTags() != null && !parseResult.getTags().isEmpty()) {
            snippet.getTags().addAll(parseResult.getTags());
        }
    }

    private String resolveLanguageType(String fileName, String languageType) {
        if (languageType != null && !languageType.isBlank()) {
            return languageType;
        }
        if (fileName.endsWith(".java")) {
            return "Java";
        }
        if (fileName.endsWith(".py")) {
            return "Python";
        }
        if (fileName.endsWith(".js") || fileName.endsWith(".ts") || fileName.endsWith(".vue")) {
            return "JavaScript/TypeScript";
        }
        return "Unknown";
    }

    private String calculateFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5DigestAsHex(fileInputStream);
        } catch (IOException e) {
            log.error("计算文件 MD5 失败: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    private void markSuccess(String rootDir) {
        if (rootDir != null) {
            scanTaskTracker.markSuccess(rootDir);
        }
    }

    private void markSkip(String rootDir) {
        if (rootDir != null) {
            scanTaskTracker.markSkip(rootDir);
        }
    }

    private void markFailed(String rootDir) {
        if (rootDir != null) {
            scanTaskTracker.markFailed(rootDir);
        }
    }
}
