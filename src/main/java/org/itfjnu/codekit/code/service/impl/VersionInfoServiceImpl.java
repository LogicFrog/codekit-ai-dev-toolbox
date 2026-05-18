package org.itfjnu.codekit.code.service.impl;

import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.ai.dto.AIChatRequest;
import org.itfjnu.codekit.ai.dto.AIChatResponse;
import org.itfjnu.codekit.ai.service.AIService;
import org.itfjnu.codekit.code.dto.*;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.code.repository.VersionInfoRepository;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.itfjnu.codekit.search.service.VectorIndexService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class VersionInfoServiceImpl implements VersionInfoService {

    private final CodeSnippetService codeSnippetService;
    private final VersionInfoRepository versionInfoRepository;
    private final CodeSnippetRepository codeSnippetRepository;
    private final VectorIndexService vectorIndexService;
    private final AIService aiService;

    @Override
    public VersionInfo createVersion(Long snippetId, CreateVersionRequest request) {
        CodeSnippet snippet = getExistingSnippet(snippetId);
        try {
            VersionInfo version = new VersionInfo();
            version.setSnippetId(snippetId);
            version.setVersionName(request.getVersionName());
            version.setCodeContent(snippet.getCodeContent());
            version.setCreateTime(LocalDateTime.now());
            version.setDescription(request.getDescription());
            return versionInfoRepository.save(version);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.VERSION_CREATE_FAILED, "创建版本失败：" + e.getMessage());
        }
    }

    @Override
    public List<VersionInfo> listVersions(Long snippetId) {
        getExistingSnippet(snippetId);
        try {
            return versionInfoRepository.findBySnippetIdOrderByCreateTimeDesc(snippetId);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.VERSION_LIST_FAILED, "查询版本失败：" + e.getMessage(), e);
        }
    }

    private CodeSnippet getExistingSnippet(Long snippetId) {
        CodeSnippet snippet = codeSnippetService.getCodeSnippetById(snippetId);
        if (snippet == null) {
            throw new BusinessException(ErrorCode.CODE_NOT_FOUND, "代码片段不存在");
        }
        return snippet;
    }

    @Override
    public VersionRollbackResponse rollbackToVersion(Long snippetId, Long versionId) {
        CodeSnippet snippet = getExistingSnippet(snippetId);
        VersionInfo target = getVersionBySnippetOrThrow(snippetId, versionId);

        try {
            // 备份当前内容
            VersionInfo backup = new VersionInfo();
            backup.setSnippetId(snippetId);
            backup.setVersionName("rollback-backup-" + DateTimeFormatter
                    .ofPattern("yyyyMMddHHmmss")
                    .format(LocalDateTime.now()));
            backup.setDescription("自动备份：回滚前快照");
            backup.setCodeContent(snippet.getCodeContent());
            backup.setCreateTime(LocalDateTime.now());
            VersionInfo backupSaved =  versionInfoRepository.save(backup);

            // 回滚
            String rollbackContent = target.getCodeContent() == null ? "" : target.getCodeContent();
            snippet.setCodeContent(rollbackContent);
            snippet.setFileMd5(DigestUtils.md5DigestAsHex(rollbackContent.getBytes(StandardCharsets.UTF_8)));
            codeSnippetRepository.save(snippet);
            vectorIndexService.upsertSnippetEmbedding(snippet);

            VersionRollbackResponse resp = new VersionRollbackResponse();

            resp.setSnippetId(snippetId);
            resp.setRollbackToVersionId(versionId);
            resp.setBackupVersionId(backupSaved.getId());
            resp.setBackupVersionName(backupSaved.getVersionName());
            resp.setRollbackTime(LocalDateTime.now());
            return resp;
        } catch (BusinessException e) {
            throw e;
        }  catch (Exception e) {
            throw new ServiceException(ErrorCode.VERSION_RESTORE_FAILED, "版本回滚失败：" + e.getMessage(), e);
        }
    }

    @Override
    public VersionDiffResponse compareVersions(Long snippetId, Long fromVersionId, Long toVersionId) {
        getExistingSnippet(snippetId);
        VersionInfo from = getVersionBySnippetOrThrow(snippetId, fromVersionId);
        VersionInfo to = getVersionBySnippetOrThrow(snippetId, toVersionId);

        try {
            return buildDiff(snippetId, from, to);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.VERSION_COMPARE_FAILED, "版本差异分析失败：" + e.getMessage(), e);
        }
    }

    @Override
    public VersionAnalyzeResponse analyzeVersions(Long snippetId, VersionAnalyzeRequest request) {
        if (request == null || request.getFromVersionId() == null || request.getToVersionId() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "fromVersionId/toVersionId 不能为空");
        }
        VersionInfo from = getVersionBySnippetOrThrow(snippetId, request.getFromVersionId());
        VersionInfo to = getVersionBySnippetOrThrow(snippetId, request.getToVersionId());
        VersionDiffResponse diff = buildDiff(snippetId, from, to);

        String prompt = buildVersionAnalyzePrompt(diff, request.getFocus());
        String contextCode = "【旧版本】\n" + safeCut(from.getCodeContent(), 6000) + "\n\n【新版本】\n"
                + safeCut(to.getCodeContent(), 6000);

        AIChatRequest aiReq = new AIChatRequest();
        aiReq.setQuestion(prompt);
        aiReq.setCode(contextCode);
        aiReq.setLanguageType("text");
        AIChatResponse aiResp = aiService.explain(aiReq);

        VersionAnalyzeResponse resp = new VersionAnalyzeResponse();
        resp.setSnippetId(snippetId);
        resp.setFromVersionId(request.getFromVersionId());
        resp.setToVersionId(request.getToVersionId());
        resp.setSummary(diff.getSummary());
        resp.setRiskLevel(guessRiskLevel(aiResp.getAnswer()));
        resp.setRawAnswer(aiResp.getAnswer());
        if (aiResp.getSuggestions() != null) {
            resp.setSuggestions(aiResp.getSuggestions());
        }
        resp.setRisks(extractRiskItems(aiResp.getAnswer()));
        resp.setTestFocus(extractTestFocusItems(aiResp.getAnswer()));
        return resp;
    }

    private VersionInfo getVersionBySnippetOrThrow(Long snippetId, Long versionId) {
        return versionInfoRepository.findByIdAndSnippetId(versionId, snippetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND, "版本不存在，ID: " + versionId));
    }

    private VersionDiffResponse buildDiff(Long snippetId, VersionInfo from, VersionInfo to) {
        String oldText = from.getCodeContent() == null ? "" : from.getCodeContent();
        String newText = to.getCodeContent() == null ? "" : to.getCodeContent();

        String[] oldLines = splitLinesWithoutTrailingBlank(oldText);
        String[] newLines = splitLinesWithoutTrailingBlank(newText);
        int[][] lcs = buildLcsTable(oldLines, newLines);
        int added = 0;
        int removed = 0;
        int modified = 0;
        int i = 0;
        int j = 0;

        List<VersionChangeBlock> blocks = new ArrayList<>();
        HunkBuilder hunk = null;

        while (i < oldLines.length || j < newLines.length) {
            if (i < oldLines.length && j < newLines.length && oldLines[i].equals(newLines[j])) {
                if (hunk != null) {
                    VersionChangeBlock block = toChangeBlock(hunk);
                    blocks.add(block);
                    if ("ADD".equals(block.getType())) {
                        added += hunk.addedLines.size();
                    } else if ("REMOVE".equals(block.getType())) {
                        removed += hunk.removedLines.size();
                    } else {
                        modified++;
                    }
                    hunk = null;
                }
                i++;
                j++;
                continue;
            }

            if (hunk == null) {
                hunk = new HunkBuilder(i + 1, j + 1);
            }

            boolean shouldRemove = j >= newLines.length
                    || (i < oldLines.length && lcs[i + 1][j] >= lcs[i][j + 1]);

            if (shouldRemove) {
                hunk.removedLines.add(oldLines[i]);
                i++;
            } else {
                hunk.addedLines.add(newLines[j]);
                j++;
            }
        }

        if (hunk != null) {
            VersionChangeBlock block = toChangeBlock(hunk);
            blocks.add(block);
            if ("ADD".equals(block.getType())) {
                added += hunk.addedLines.size();
            } else if ("REMOVE".equals(block.getType())) {
                removed += hunk.removedLines.size();
            } else {
                modified++;
            }
        }

        int totalBase = Math.max(oldLines.length, 1);
        double rate = ((double) (added + removed + modified)) / totalBase;

        VersionDiffResponse resp = new org.itfjnu.codekit.code.dto.VersionDiffResponse();
        resp.setSnippetId(snippetId);
        resp.setFromVersionId(from.getId());
        resp.setToVersionId(to.getId());
        resp.setAddedLines(added);
        resp.setRemovedLines(removed);
        resp.setModifiedBlocks(modified);
        resp.setChangeRate(Math.round(rate * 10000d) / 10000d);
        resp.setSummary(String.format("新增 %d 行，删除 %d 行，修改 %d 处", added, removed, modified));
        resp.setBlocks(blocks);
        return resp;
    }

    private String buildVersionAnalyzePrompt(org.itfjnu.codekit.code.dto.VersionDiffResponse diff, String focus) {
        String finalFocus = (focus == null || focus.isBlank()) ? "通用质量、潜在风险、测试建议" : focus;
        return "你是资深代码评审工程师。请基于两个版本差异进行分析。\n"
                + "要求：\n"
                + "1) 用中文输出；\n"
                + "2) 先给总体结论；\n"
                + "3) 列出主要风险点；\n"
                + "4) 给出可执行改进建议；\n"
                + "5) 给出测试关注点。\n\n"
                + "差异统计：" + diff.getSummary() + "\n"
                + "关注重点：" + finalFocus;
    }

    private String guessRiskLevel(String answer) {
        if (answer == null || answer.isBlank()) {
            return "UNKNOWN";
        }
        String text = answer.toLowerCase();
        if (text.contains("高风险") || text.contains("严重") || text.contains("critical")) {
            return "HIGH";
        }
        if (text.contains("中风险") || text.contains("注意") || text.contains("medium")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String safeCut(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String[] splitLinesWithoutTrailingBlank(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        String[] raw = text.split("\\R", -1);
        int end = raw.length;
        while (end > 0 && raw[end - 1].isEmpty()) {
            end--;
        }
        return Arrays.copyOf(raw, end);
    }

    private int[][] buildLcsTable(String[] oldLines, String[] newLines) {
        int n = oldLines.length;
        int m = newLines.length;
        int[][] dp = new int[n + 1][m + 1];

        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (oldLines[i].equals(newLines[j])) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        return dp;
    }

    private VersionChangeBlock toChangeBlock(HunkBuilder hunk) {
        VersionChangeBlock block = new VersionChangeBlock();
        int removeSize = hunk.removedLines.size();
        int addSize = hunk.addedLines.size();

        if (removeSize == 0) {
            block.setType("ADD");
            block.setOldStartLine(Math.max(0, hunk.oldStartLine - 1));
            block.setOldEndLine(Math.max(0, hunk.oldStartLine - 1));
            block.setNewStartLine(hunk.newStartLine);
            block.setNewEndLine(hunk.newStartLine + addSize - 1);
            block.setOldSnippet("");
            block.setNewSnippet(String.join("\n", hunk.addedLines));
            return block;
        }

        if (addSize == 0) {
            block.setType("REMOVE");
            block.setOldStartLine(hunk.oldStartLine);
            block.setOldEndLine(hunk.oldStartLine + removeSize - 1);
            block.setNewStartLine(Math.max(0, hunk.newStartLine - 1));
            block.setNewEndLine(Math.max(0, hunk.newStartLine - 1));
            block.setOldSnippet(String.join("\n", hunk.removedLines));
            block.setNewSnippet("");
            return block;
        }

        block.setType("MODIFY");
        block.setOldStartLine(hunk.oldStartLine);
        block.setOldEndLine(hunk.oldStartLine + removeSize - 1);
        block.setNewStartLine(hunk.newStartLine);
        block.setNewEndLine(hunk.newStartLine + addSize - 1);
        block.setOldSnippet(String.join("\n", hunk.removedLines));
        block.setNewSnippet(String.join("\n", hunk.addedLines));
        return block;
    }

    private List<String> extractRiskItems(String answer) {
        return extractItemsByKeywords(answer, List.of("风险", "隐患", "问题", "异常", "兼容"));
    }

    private List<String> extractTestFocusItems(String answer) {
        return extractItemsByKeywords(answer, List.of("测试", "验证", "回归", "边界", "用例"));
    }

    private List<String> extractItemsByKeywords(String answer, List<String> keywords) {
        List<String> result = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            return result;
        }

        String[] lines = answer.split("\\R");
        for (String line : lines) {
            String item = sanitizeBulletLine(line);
            if (item.isBlank()) {
                continue;
            }
            String lower = item.toLowerCase(Locale.ROOT);
            boolean matched = keywords.stream().anyMatch(k ->
                    item.contains(k) || lower.contains(k.toLowerCase(Locale.ROOT)));
            if (matched && result.stream().noneMatch(item::equals)) {
                result.add(item);
            }
        }
        return result;
    }

    private String sanitizeBulletLine(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceFirst("^[\\-\\*•\\d\\.\\)\\s]+", "").trim();
    }

    private static class HunkBuilder {
        private final int oldStartLine;
        private final int newStartLine;
        private final List<String> removedLines = new ArrayList<>();
        private final List<String> addedLines = new ArrayList<>();

        private HunkBuilder(int oldStartLine, int newStartLine) {
            this.oldStartLine = oldStartLine;
            this.newStartLine = newStartLine;
        }
    }
}
