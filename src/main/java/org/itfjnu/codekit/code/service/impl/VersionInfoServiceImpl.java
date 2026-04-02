package org.itfjnu.codekit.code.service.impl;

import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.dto.CreateVersionRequest;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.model.VersionInfo;
import org.itfjnu.codekit.code.repository.VersionInfoRepository;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.itfjnu.codekit.code.service.VersionInfoService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.itfjnu.codekit.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class VersionInfoServiceImpl implements VersionInfoService {

    private final CodeSnippetService codeSnippetService;
    private final VersionInfoRepository versionInfoRepository;

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
}
