package org.itfjnu.codekit.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.search.model.CodeEmbedding;
import org.itfjnu.codekit.search.repository.CodeEmbeddingRepository;
import org.itfjnu.codekit.search.service.EmbeddingService;
import org.itfjnu.codekit.search.service.VectorIndexService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexServiceImpl implements VectorIndexService {
    private static final int DEFAULT_MAX_SCAN = 5000;
    private static final int MAX_CANDIDATE_SCAN = 8000;

    private final CodeEmbeddingRepository codeEmbeddingRepository;
    private final CodeSnippetRepository codeSnippetRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    private String buildText(CodeSnippet snippet) {
        String tagsStr = snippet.getTags() == null ? "" : String.join(" ", snippet.getTags());
        return String.join(" ",
                safe(snippet.getFileName()),
                safe(snippet.getClassName()),
                safe(snippet.getPackageName()),
                tagsStr,
                safe(snippet.getCodeContent())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }


    @Override
    @Transactional
    public Boolean upsertSnippetEmbedding(CodeSnippet snippet) {
        try {
            String text = buildText(snippet);
            List<Double> vec = embeddingService.embedText(text);

            CodeEmbedding em = codeEmbeddingRepository.findBySnippetId(snippet.getId())
                    .orElse(new CodeEmbedding());

            em.setSnippetId(snippet.getId());
            em.setEmbeddingJson(objectMapper.writeValueAsString(vec));
            em.setEmbeddingDim(vec.size());
            em.setModelName(embeddingService.modelName());

            codeEmbeddingRepository.save(em);
            return true;
        } catch (Exception e) {
            log.error("向量更新失败", e);
            throw new BusinessException(ErrorCode.SEARCH_INDEX_UPDATE_FAILED);
        }
    }

    @Override
    @Transactional
    public Boolean deleteSnippetEmbedding(Long snippetId) {
        try {
            codeEmbeddingRepository.deleteBySnippetId(snippetId);
            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SEARCH_INDEX_UPDATE_FAILED);
        }
    }

    @Override
    public List<Long> searchTopKByText(String query, int topK) {
        return searchTopKByText(query, topK, null);
    }

    @Override
    public List<Long> searchTopKByText(String query, int topK, Collection<Long> candidateSnippetIds) {
        try {
            List<Double> queryVec = embeddingService.embedText(query);
            if (queryVec == null || queryVec.isEmpty()) {
                return List.of();
            }
            int effectiveTopK = Math.max(1, topK);
            List<CodeEmbedding> all = loadEmbeddingsForSearch(candidateSnippetIds);
            if (all.isEmpty()) {
                return List.of();
            }
            List<Map.Entry<Long, Double>> scores = new ArrayList<>();

            for (CodeEmbedding em : all) {
                List<Double> vec = objectMapper.readValue(
                        em.getEmbeddingJson(),
                        new TypeReference<>() {
                        }
                );
                double sim = cosineSimilarity(queryVec, vec);
                scores.add(new AbstractMap.SimpleEntry<>(em.getSnippetId(), sim));
            }

            scores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            return scores.stream()
                    .limit(effectiveTopK)
                    .map(Map.Entry::getKey)
                    .toList();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("语义检索失败", e);
            throw new BusinessException(ErrorCode.SEARCH_FAILED);
        }
    }

    private List<CodeEmbedding> loadEmbeddingsForSearch(Collection<Long> candidateSnippetIds) {
        if (candidateSnippetIds != null && !candidateSnippetIds.isEmpty()) {
            Set<Long> candidateSet = new LinkedHashSet<>(candidateSnippetIds);
            if (candidateSet.size() > MAX_CANDIDATE_SCAN) {
                candidateSet = candidateSet.stream()
                        .limit(MAX_CANDIDATE_SCAN)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                log.info("语义候选集过大，已裁剪到 {} 条", MAX_CANDIDATE_SCAN);
            }
            return codeEmbeddingRepository.findBySnippetIdIn(candidateSet);
        }
        return codeEmbeddingRepository.findAllByOrderByUpdateTimeDesc(PageRequest.of(0, DEFAULT_MAX_SCAN))
                .getContent();
    }

    @Override
    @Transactional
    public Boolean rebuildAllEmbeddings() {
        List<CodeSnippet> list = codeSnippetRepository.findAll();
        for (CodeSnippet snip : list) {
            upsertSnippetEmbedding(snip);
        }
        return true;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int n = Math.min(a.size(), b.size());
        if (n == 0) {
            return 0.0;
        }
        double dot = 0, ma = 0, mb = 0;
        for (int i = 0; i < n; i++) {
            dot += a.get(i) * b.get(i);
            ma += a.get(i) * a.get(i);
            mb += b.get(i) * b.get(i);
        }
        return dot / (Math.sqrt(ma) * Math.sqrt(mb) + 1e-8);
    }
}
