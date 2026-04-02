package org.itfjnu.codekit.code.filesystem.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class JavaCodeMetadataParser implements CodeMetadataParser {

    @Override
    public boolean supports(String languageType) {
        return "Java".equalsIgnoreCase(languageType);
    }

    @Override
    public CodeParseResult parse(CodeSnippet snippet) {
        if (snippet.getCodeContent() == null || snippet.getCodeContent().isBlank()) {
            return CodeParseResult.builder().build();
        }

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(snippet.getCodeContent());
            Set<String> tags = new HashSet<>();
            Set<String> imports = new HashSet<>();

            String packageName = compilationUnit.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse(null);

            List<ClassOrInterfaceDeclaration> classes = compilationUnit.findAll(ClassOrInterfaceDeclaration.class);
            String className = null;
            if (!classes.isEmpty()) {
                className = classes.get(0).getNameAsString();
                for (int i = 1; i < classes.size(); i++) {
                    tags.add("innerClass:" + classes.get(i).getNameAsString());
                }
            }

            compilationUnit.findAll(MethodDeclaration.class)
                    .forEach(method -> tags.add("method:" + method.getNameAsString()));

            compilationUnit.getImports()
                    .forEach(importDeclaration -> imports.add(importDeclaration.getNameAsString()));

            return CodeParseResult.builder()
                    .packageName(packageName)
                    .className(className)
                    .tags(tags)
                    .imports(List.copyOf(imports))
                    .build();
        } catch (Exception e) {
            log.warn("【语法解析跳过】文件 {} 存在语法错误或不完整，仅存储原始内容", snippet.getFilePath());
            return CodeParseResult.builder().build();
        }
    }
}
