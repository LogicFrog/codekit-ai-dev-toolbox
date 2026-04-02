package org.itfjnu.codekit.code.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 代码片段实体：对应MySQL的code_snippet表
 */
@Entity
@Table(name = "code_snippet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSnippet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键自增

    @Column(name = "file_path", nullable = false, length = 512, unique = true)
    private String filePath; // 代码文件绝对路径，非空

    @Column(name = "file_name", nullable = false)
    private String fileName; // 文件名，非空

    @Column(name = "code_content", columnDefinition = "LONGTEXT")
    private String codeContent; // 代码内容，长文本

    @Column(name = "language_type")
    private String languageType; // 开发语言：Java/Python/JS等

    @Column(name = "file_md5", length = 32)
    private String fileMd5; // 文件内容MD5，用于查重和处理路径变更

    @Column(name = "package_name", length = 255)
    private String packageName; // 包名，如 org.itfjnu.codekit

    @Column(name = "class_name", length = 128)
    private String className;   // 类名，如 LocalFileScanService

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private CodeCategory category; // 所属分类

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "code_snippet_tags", joinColumns = @JoinColumn(name = "snippet_id"))
    @Column(name = "tag_name")
    private Set<String> tags = new HashSet<>(); // 标签集合，优化检索性能

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime; // 创建时间，不更新

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间

    // 自动填充创建/更新时间（JPA生命周期注解）
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
