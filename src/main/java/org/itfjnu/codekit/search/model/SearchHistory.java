package org.itfjnu.codekit.search.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "search_history")
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 检索关键词 (如 "Redis连接")
    @Column(name = "keyword", nullable = false, length = 200)
    private String keyword;

    // 检索类型 (0:关键词检索, 1:语义检索)
    @Column(name = "search_type")
    private Integer searchType;

    // 检索时间
    @Column(name = "search_time")
    private LocalDateTime searchTime;

    @PrePersist
    public void prePersist() {
        this.searchTime = LocalDateTime.now();
    }
}
