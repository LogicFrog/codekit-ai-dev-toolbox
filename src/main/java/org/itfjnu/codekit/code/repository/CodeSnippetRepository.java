package org.itfjnu.codekit.code.repository;

import org.itfjnu.codekit.code.model.CodeSnippet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, Long> {

    /**
     * 根据文件路径查询代码片段
     * @param filePath 文件路径
     * @return Optional<CodeSnippet>，存在则返回，不存在返回 Optional.empty()
     */
    Optional<CodeSnippet> findByFilePath(String filePath);

    CodeSnippet findByFileMd5(String fileMd5);

    void deleteByFilePath(String filePath);

    List<CodeSnippet> findByLanguageType(String languageType);

    List<CodeSnippet> findByCategory_Id(Long categoryId);

    Page<CodeSnippet> findByCategory_Id(Long categoryId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM CodeSnippet s JOIN s.tags t WHERE t = :tag")
    List<CodeSnippet> findByTagName(String tag);

    @Query("SELECT DISTINCT s FROM CodeSnippet s JOIN s.tags t WHERE s.languageType = :languageType AND t = :tag")
    List<CodeSnippet> findByLanguageTypeAndTagName(@Param("languageType") String languageType, @Param("tag") String tag);

    /**
     * 全文搜索（关键词检索）
     * 使用 MySQL FULLTEXT 索引
     */
    @Query(value = "SELECT * FROM code_snippet " +
            "WHERE MATCH(file_name, class_name, code_content) " +
            "AGAINST(:keyword IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    List<CodeSnippet> fullTextSearch(@Param("keyword") String keyword);

    /**
     * 全文搜索 + 语言类型过滤
     */
    @Query(value = "SELECT * FROM code_snippet " +
            "WHERE MATCH(file_name, class_name, code_content) " +
            "AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "AND language_type = :languageType",
            nativeQuery = true)
    List<CodeSnippet> fullTextSearchByLanguage(
            @Param("keyword") String keyword,
            @Param("languageType") String languageType
    );

    /**
     * 全文搜索 + 标签过滤
     */
    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE MATCH(s.file_name, s.class_name, s.code_content) " +
            "AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> fullTextSearchByTag(
            @Param("keyword") String keyword,
            @Param("tag") String tag
    );

    /**
     * 全文搜索 + 语言类型 + 标签过滤
     */
    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE MATCH(s.file_name, s.class_name, s.code_content) " +
            "AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "AND s.language_type = :languageType " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> fullTextSearchByLanguageAndTag(
            @Param("keyword") String keyword,
            @Param("languageType") String languageType,
            @Param("tag") String tag
    );

    /**
     * 布尔模式全文搜索（支持 + - ~ 等操作符）
     */
    @Query(value = "SELECT * FROM code_snippet " +
            "WHERE MATCH(file_name, class_name, code_content) " +
            "AGAINST(:keyword IN BOOLEAN MODE)",
            nativeQuery = true)
    List<CodeSnippet> fullTextSearchBoolean(@Param("keyword") String keyword);

    /**
     * 精确匹配查询（LIKE 查询）
     */
    List<CodeSnippet> findByCodeContentContaining(String keyword);

    List<CodeSnippet> findByFileNameContaining(String fileName);

    List<CodeSnippet> findByClassNameContaining(String className);

    List<CodeSnippet> findByLanguageTypeAndCodeContentContaining(String languageType, String keyword);

    List<CodeSnippet> findByLanguageTypeAndFileNameContaining(String languageType, String fileName);

    List<CodeSnippet> findByLanguageTypeAndClassNameContaining(String languageType, String className);

    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE s.code_content LIKE CONCAT('%', :keyword, '%') " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> findByTagAndCodeContentContaining(
            @Param("tag") String tag,
            @Param("keyword") String keyword
    );

    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE s.file_name LIKE CONCAT('%', :keyword, '%') " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> findByTagAndFileNameContaining(
            @Param("tag") String tag,
            @Param("keyword") String keyword
    );

    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE s.class_name LIKE CONCAT('%', :keyword, '%') " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> findByTagAndClassNameContaining(
            @Param("tag") String tag,
            @Param("keyword") String keyword
    );

    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE s.code_content LIKE CONCAT('%', :keyword, '%') " +
            "AND s.language_type = :languageType " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> findByLanguageTypeAndTagAndCodeContentContaining(
            @Param("languageType") String languageType,
            @Param("tag") String tag,
            @Param("keyword") String keyword
    );

    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE s.file_name LIKE CONCAT('%', :keyword, '%') " +
            "AND s.language_type = :languageType " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> findByLanguageTypeAndTagAndFileNameContaining(
            @Param("languageType") String languageType,
            @Param("tag") String tag,
            @Param("keyword") String keyword
    );

    @Query(value = "SELECT s.* FROM code_snippet s " +
            "JOIN code_snippet_tags t ON s.id = t.snippet_id " +
            "WHERE s.class_name LIKE CONCAT('%', :keyword, '%') " +
            "AND s.language_type = :languageType " +
            "AND t.tag_name = :tag",
            nativeQuery = true)
    List<CodeSnippet> findByLanguageTypeAndTagAndClassNameContaining(
            @Param("languageType") String languageType,
            @Param("tag") String tag,
            @Param("keyword") String keyword
    );
}
