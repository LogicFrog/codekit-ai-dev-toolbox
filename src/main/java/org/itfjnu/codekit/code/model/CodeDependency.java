package org.itfjnu.codekit.code.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

/**
 * 代码依赖关系实体：对应MySQL的code_dependency表
 * 关联代码片段，存储依赖类/方法
 */
@Entity
@Table(name = "code_dependency")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键自增

    @Column(name = "code_snippet_id", nullable = false)
    private Long codeSnippetId; // 关联的代码片段ID，非空

    @Column(name = "depend_name", nullable = false)
    private String dependName; // 依赖类/方法名，如RedisTemplate

    @Column(name = "depend_type")
    private String dependType; // 依赖类型：CLASS/METHOD/PACKAGE等
}
