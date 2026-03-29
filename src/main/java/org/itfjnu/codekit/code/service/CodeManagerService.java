package org.itfjnu.codekit.code.service;

import org.itfjnu.codekit.code.model.CodeDependency;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 代码管理模块核心服务接口
 */
public interface CodeManagerService {
    // 新增/更新代码片段（存在则更新，不存在则新增）
    CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet codeSnippet);

    // 根据ID删除代码片段（联动删除依赖），返回是否删除成功
    Boolean deleteCodeSnippetById(Long id);

    // 根据ID查询代码片段
    CodeSnippet getCodeSnippetById(Long id);

    // 分页查询所有代码片段
    Page<CodeSnippet> listCodeSnippetByPage(Pageable pageable);

    // 根据语言类型查询
    List<CodeSnippet> listCodeSnippetByLanguage(String languageType);

    // 根据标签模糊查询
    List<CodeSnippet> listCodeSnippetByTag(String tag);

    // 检查文件路径是否已存在
    Boolean isFilePathExists(String filePath);

    // 根据路径获取代码片段
    CodeSnippet getCodeSnippetByPath(String filePath);

    // 根据 MD5 查询代码片段
    CodeSnippet getCodeSnippetByMd5(String fileMd5);

    // 根据文件路径删除
    Boolean deleteByFilePath(String filePath);

    // 获取数据库中所有的文件路径，用于清理僵尸数据
    List<String> getAllFilePaths();

    // 根据代码片段查询所有依赖
    List<CodeDependency> listDependenciesBySnippetId(Long snippetId);

    // 保存代码依赖关系
    Boolean saveDependencies(Long snippetId, List<String> importList);

}
