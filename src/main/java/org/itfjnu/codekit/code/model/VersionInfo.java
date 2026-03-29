package org.itfjnu.codekit.code.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "version_info")
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的代码片段ID
    @Column(name = "snippet_id", nullable = false)
    private Long snippetId;

    // 版本号/标识 (如 v1.0, v1.1)
    @Column(name = "version_name", length = 50)
    private String versionName;

    // 版本描述 (如 "修复了空指针异常")
    @Column(name = "description", length = 500)
    private String description;

    // 代码内容快照 (存当时的代码)
    @Lob
    @Column(name = "code_content", columnDefinition = "TEXT")
    private String codeContent;

    // 创建时间
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
