package org.itfjnu.codekit.code.service;

import org.itfjnu.codekit.code.model.CodeDependency;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CodeSnippetService {

    CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet codeSnippet);

    Boolean deleteCodeSnippetById(Long id);

    CodeSnippet getCodeSnippetById(Long id);

    Page<CodeSnippet> listCodeSnippetByPage(Pageable pageable);

    Page<CodeSnippet> listCodeSnippetByCategory(Long categoryId, Pageable pageable);

    List<CodeSnippet> listCodeSnippetByLanguage(String languageType);

    List<CodeSnippet> listCodeSnippetByTag(String tag);

    List<CodeSnippet> listCodeSnippetByCategory(Long categoryId);

    CodeSnippet assignCategory(Long snippetId, Long categoryId);

    Boolean isFilePathExists(String filePath);

    CodeSnippet getCodeSnippetByPath(String filePath);

    CodeSnippet getCodeSnippetByMd5(String fileMd5);

    Boolean deleteByFilePath(String filePath);

    List<String> getAllFilePaths();

    List<CodeDependency> listDependenciesBySnippetId(Long snippetId);

    Boolean saveDependencies(Long snippetId, List<String> importList);
}
