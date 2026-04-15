package org.itfjnu.codekit.code.repository;

import org.itfjnu.codekit.code.model.VersionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
