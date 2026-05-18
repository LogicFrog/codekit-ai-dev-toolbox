package org.itfjnu.codekit.code.repository;

import org.itfjnu.codekit.code.model.VersionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 版本信息 Repository
 */
@Repository
public interface VersionInfoRepository extends JpaRepository<VersionInfo, Long> {

    /**
     * 根据代码片段 ID 查询所有版本
     * @param snippetId 代码片段 ID
     * @return 版本列表
     */
    List<VersionInfo> findBySnippetIdOrderByCreateTimeDesc(Long snippetId);

    /**
     * 根据代码片段 ID 删除所有版本
     * @param snippetId 代码片段 ID
     */
    long deleteBySnippetId(Long snippetId);

    /**
     * 根据版本ID和代码片段ID精准查询版本信息
     * @param id         版本ID
     * @param snippetId  代码片段ID
     * @return Optional<VersionInfo> 匹配的版本信息
     */
    Optional<VersionInfo> findByIdAndSnippetId(Long id, Long snippetId);
}
