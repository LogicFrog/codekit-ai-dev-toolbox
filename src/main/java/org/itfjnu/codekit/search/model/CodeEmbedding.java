package org.itfjnu.codekit.search.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "code_embedding")
@AllArgsConstructor
@NoArgsConstructor
public class CodeEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "snippet_id", nullable = false, unique = true)
    private Long snippetId;

    @Column(name = "embedding_json", nullable = false, columnDefinition = "longtext")
    private String embeddingJson;

    @Column(name = "embedding_dim", nullable = false)
    private Integer embeddingDim;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

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
