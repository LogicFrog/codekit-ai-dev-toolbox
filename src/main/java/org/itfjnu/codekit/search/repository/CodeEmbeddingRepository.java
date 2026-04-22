package org.itfjnu.codekit.search.repository;

import org.itfjnu.codekit.search.model.CodeEmbedding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeEmbeddingRepository extends JpaRepository<CodeEmbedding, Long> {
    Optional<CodeEmbedding> findBySnippetId(Long snippetId);
    void deleteBySnippetId(Long snippetId);
    List<CodeEmbedding> findAll();
    List<CodeEmbedding> findBySnippetIdIn(Collection<Long> snippetIds);
    Page<CodeEmbedding> findAllByOrderByUpdateTimeDesc(Pageable pageable);
}
