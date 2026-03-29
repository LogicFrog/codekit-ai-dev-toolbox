package org.itfjnu.codekit.code.repository;

import org.itfjnu.codekit.code.model.CodeDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码依赖关系数据访问层
 */
@Repository
public interface CodeDependencyRepository extends JpaRepository<CodeDependency, Long> {
    // 自定义查询：根据代码片段ID查询所有依赖
    List<CodeDependency> findByCodeSnippetId(Long codeSnippetId);

    // 删除指定代码片段的所有依赖（代码删除时联动）
    void deleteByCodeSnippetId(Long codeSnippetId);
}
