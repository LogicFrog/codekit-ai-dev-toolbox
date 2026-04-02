package org.itfjnu.codekit.search.service.support;

import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.search.dto.SearchResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SearchResponseAssembler {

    public List<SearchResponse> assemble(List<CodeSnippet> snippets, String keyword) {
        return snippets.stream()
                .map(snippet -> toSearchResponse(snippet, keyword))
                .sorted(Comparator.comparing(SearchResponse::getRelevanceScore).reversed())
                .toList();
    }

    public SearchResponse toSearchResponse(CodeSnippet snippet, String keyword) {
        PreviewResult previewResult = buildPreview(snippet.getCodeContent(), keyword);
        return SearchResponse.builder()
                .id(snippet.getId())
                .filePath(snippet.getFilePath())
                .fileName(snippet.getFileName())
                .codePreview(previewResult.codePreview())
                .highlight(previewResult.highlight())
                .languageType(snippet.getLanguageType())
                .className(snippet.getClassName())
                .packageName(snippet.getPackageName())
                .tags(snippet.getTags())
                .relevanceScore(calculateRelevanceScore(snippet, keyword))
                .createTime(snippet.getCreateTime())
                .build();
    }

    private PreviewResult buildPreview(String content, String keyword) {
        if (content == null) {
            return new PreviewResult("", "");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return new PreviewResult(content.substring(0, Math.min(200, content.length())), "");
        }

        String lowerKeyword = keyword.toLowerCase();
        String lowerContent = content.toLowerCase();
        int keywordIndex = lowerContent.indexOf(lowerKeyword);

        if (keywordIndex < 0) {
            return new PreviewResult(content.substring(0, Math.min(200, content.length())), "");
        }

        int start = Math.max(0, keywordIndex - 100);
        int end = Math.min(content.length(), keywordIndex + keyword.length() + 100);
        return new PreviewResult(
                content.substring(start, end),
                content.substring(keywordIndex, keywordIndex + keyword.length())
        );
    }

    private double calculateRelevanceScore(CodeSnippet snippet, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String lowerKeyword = keyword.toLowerCase();

        if (snippet.getFileName() != null && snippet.getFileName().toLowerCase().contains(lowerKeyword)) {
            score += 1.0;
        }
        if (snippet.getClassName() != null && snippet.getClassName().toLowerCase().contains(lowerKeyword)) {
            score += 0.8;
        }
        if (snippet.getPackageName() != null && snippet.getPackageName().toLowerCase().contains(lowerKeyword)) {
            score += 0.6;
        }
        if (snippet.getCodeContent() != null && snippet.getCodeContent().toLowerCase().contains(lowerKeyword)) {
            score += 0.4;
        }
        if (snippet.getTags() != null
                && snippet.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword))) {
            score += 0.5;
        }

        return Math.min(score, 1.0);
    }

    private record PreviewResult(String codePreview, String highlight) {
    }
}
