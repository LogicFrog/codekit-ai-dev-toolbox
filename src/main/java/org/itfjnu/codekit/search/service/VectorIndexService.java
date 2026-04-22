package org.itfjnu.codekit.search.service;

import org.itfjnu.codekit.code.model.CodeSnippet;

import java.util.Collection;
import java.util.List;

/**
 * Service for vector index management and semantic search of code snippets
 */
public interface VectorIndexService {

    /**
     * Create or update the vector embedding for a code snippet
     * @param snippet code snippet to be embedded
     * @return true if operation succeeded, false otherwise
     */
    Boolean upsertSnippetEmbedding(CodeSnippet snippet);

    /**
     * Delete the vector embedding of a code snippet by its ID
     * @param snippetId ID of the code snippet
     * @return true if operation succeeded, false otherwise
     */
    Boolean deleteSnippetEmbedding(Long snippetId);

    /**
     * Search top-K most similar code snippets using text query semantic matching
     * @param query text search query
     * @param topK number of results to return
     * @return list of matched code snippet IDs
     */
    List<Long> searchTopKByText(String query, int topK);

    /**
     * Search top-K most similar code snippets in candidate snippet IDs.
     * @param query text search query
     * @param topK number of results to return
     * @param candidateSnippetIds optional candidate snippet ID set for pre-filtering
     * @return list of matched code snippet IDs
     */
    List<Long> searchTopKByText(String query, int topK, Collection<Long> candidateSnippetIds);

    /**
     * Rebuild vector embeddings for all code snippets
     * @return true if rebuild succeeded, false otherwise
     */
    Boolean rebuildAllEmbeddings();
}
