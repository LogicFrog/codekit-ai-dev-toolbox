package org.itfjnu.codekit.search.service.support;

import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.search.dto.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchQueryExecutor {

    private final CodeSnippetRepository codeSnippetRepository;

    public List<CodeSnippet> loadSnippetsByRequest(SearchRequest request) {
        if (!hasKeyword(request)) {
            return loadFilteredSnippets(request);
        }
        if (Boolean.TRUE.equals(request.getExactMatch())) {
            return loadExactMatchSnippets(request);
        }
        return loadFullTextSnippets(request);
    }

    private List<CodeSnippet> loadFilteredSnippets(SearchRequest request) {
        if (request.getLanguageType() != null && request.getTag() != null) {
            return codeSnippetRepository.findByLanguageTypeAndTagName(request.getLanguageType(), request.getTag());
        }
        if (request.getLanguageType() != null) {
            return codeSnippetRepository.findByLanguageType(request.getLanguageType());
        }
        if (request.getTag() != null) {
            return codeSnippetRepository.findByTagName(request.getTag());
        }
        return List.of();
    }

    private List<CodeSnippet> loadExactMatchSnippets(SearchRequest request) {
        List<CodeSnippet> snippets = new ArrayList<>();
        snippets.addAll(loadExactContentMatches(request));
        snippets.addAll(loadExactFileNameMatches(request));
        snippets.addAll(loadExactClassNameMatches(request));
        return snippets.stream().distinct().collect(Collectors.toList());
    }

    private List<CodeSnippet> loadExactContentMatches(SearchRequest request) {
        if (request.getLanguageType() != null && request.getTag() != null) {
            return codeSnippetRepository.findByLanguageTypeAndTagAndCodeContentContaining(
                    request.getLanguageType(), request.getTag(), request.getKeyword());
        }
        if (request.getLanguageType() != null) {
            return codeSnippetRepository.findByLanguageTypeAndCodeContentContaining(
                    request.getLanguageType(), request.getKeyword());
        }
        if (request.getTag() != null) {
            return codeSnippetRepository.findByTagAndCodeContentContaining(
                    request.getTag(), request.getKeyword());
        }
        return codeSnippetRepository.findByCodeContentContaining(request.getKeyword());
    }

    private List<CodeSnippet> loadExactFileNameMatches(SearchRequest request) {
        if (request.getLanguageType() != null && request.getTag() != null) {
            return codeSnippetRepository.findByLanguageTypeAndTagAndFileNameContaining(
                    request.getLanguageType(), request.getTag(), request.getKeyword());
        }
        if (request.getLanguageType() != null) {
            return codeSnippetRepository.findByLanguageTypeAndFileNameContaining(
                    request.getLanguageType(), request.getKeyword());
        }
        if (request.getTag() != null) {
            return codeSnippetRepository.findByTagAndFileNameContaining(
                    request.getTag(), request.getKeyword());
        }
        return codeSnippetRepository.findByFileNameContaining(request.getKeyword());
    }

    private List<CodeSnippet> loadExactClassNameMatches(SearchRequest request) {
        if (request.getLanguageType() != null && request.getTag() != null) {
            return codeSnippetRepository.findByLanguageTypeAndTagAndClassNameContaining(
                    request.getLanguageType(), request.getTag(), request.getKeyword());
        }
        if (request.getLanguageType() != null) {
            return codeSnippetRepository.findByLanguageTypeAndClassNameContaining(
                    request.getLanguageType(), request.getKeyword());
        }
        if (request.getTag() != null) {
            return codeSnippetRepository.findByTagAndClassNameContaining(
                    request.getTag(), request.getKeyword());
        }
        return codeSnippetRepository.findByClassNameContaining(request.getKeyword());
    }

    private List<CodeSnippet> loadFullTextSnippets(SearchRequest request) {
        if (request.getLanguageType() != null && request.getTag() != null) {
            return codeSnippetRepository.fullTextSearchByLanguageAndTag(
                    request.getKeyword(), request.getLanguageType(), request.getTag());
        }
        if (request.getLanguageType() != null) {
            return codeSnippetRepository.fullTextSearchByLanguage(
                    request.getKeyword(), request.getLanguageType());
        }
        if (request.getTag() != null) {
            return codeSnippetRepository.fullTextSearchByTag(request.getKeyword(), request.getTag());
        }
        return codeSnippetRepository.fullTextSearch(request.getKeyword());
    }

    private boolean hasKeyword(SearchRequest request) {
        return request.getKeyword() != null && !request.getKeyword().trim().isEmpty();
    }
}
