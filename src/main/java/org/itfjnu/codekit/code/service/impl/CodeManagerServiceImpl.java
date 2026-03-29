package org.itfjnu.codekit.code.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeDependency;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeDependencyRepository;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.code.repository.VersionInfoRepository;
import org.itfjnu.codekit.code.service.CodeManagerService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CodeManagerServiceImpl implements CodeManagerService {

    private final CodeSnippetRepository codeSnippetRepository;
    private final CodeDependencyRepository codeDependencyRepository;
    private final VersionInfoRepository versionInfoRepository;

    @Override
    public CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet snippet) {
        log.debug("保存或更新代码片段：filePath={}, id={}", snippet.getFilePath(), snippet.getId());
        
        // 1. 如果 id 存在，优先按 id 更新
        if (snippet.getId() != null) {
            Optional<CodeSnippet> existingOpt = codeSnippetRepository.findById(snippet.getId());
            if (existingOpt.isPresent()) {
                CodeSnippet existing = existingOpt.get();
                existing.setCodeContent(snippet.getCodeContent());
                existing.setFileMd5(snippet.getFileMd5());
                existing.setFilePath(snippet.getFilePath());
                existing.setFileName(snippet.getFileName());
                existing.setLanguageType(snippet.getLanguageType());
                if (snippet.getPackageName() != null) {
                    existing.setPackageName(snippet.getPackageName());
                }
                if (snippet.getClassName() != null) {
                    existing.setClassName(snippet.getClassName());
                }
                if (snippet.getTags() != null && !snippet.getTags().isEmpty()) {
                    existing.setTags(snippet.getTags());
                }
                CodeSnippet saved = codeSnippetRepository.save(existing);
                log.info("按 ID 更新代码片段成功：id={}, filePath={}", saved.getId(), saved.getFilePath());
                return saved;
            } else {
                log.warn("按 ID 未找到代码片段：id={}", snippet.getId());
                throw new BusinessException(ErrorCode.CODE_NOT_FOUND, "代码片段不存在，ID: " + snippet.getId());
            }
        }

        // 2. 如果 id 不存在但 filePath 存在，按 filePath 更新
        if (snippet.getFilePath() != null) {
            Optional<CodeSnippet> existingByPath = codeSnippetRepository.findByFilePath(snippet.getFilePath());
            if (existingByPath.isPresent()) {
                CodeSnippet existing = existingByPath.get();
                existing.setCodeContent(snippet.getCodeContent());
                existing.setFileMd5(snippet.getFileMd5());
                existing.setFilePath(snippet.getFilePath());
                existing.setFileName(snippet.getFileName());
                existing.setLanguageType(snippet.getLanguageType());
                if (snippet.getPackageName() != null) {
                    existing.setPackageName(snippet.getPackageName());
                }
                if (snippet.getClassName() != null) {
                    existing.setClassName(snippet.getClassName());
                }
                if (snippet.getTags() != null && !snippet.getTags().isEmpty()) {
                    existing.setTags(snippet.getTags());
                }
                CodeSnippet saved = codeSnippetRepository.save(existing);
                log.info("按路径更新代码片段成功：id={}, filePath={}", saved.getId(), saved.getFilePath());
                return saved;
            }
        }

        // 3. 都不存在，才新建
        CodeSnippet saved = codeSnippetRepository.save(snippet);
        log.info("新建代码片段成功：id={}, filePath={}", saved.getId(), saved.getFilePath());
        return saved;
    }

    @Override
    public Boolean deleteCodeSnippetById(Long id) {
        codeSnippetRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND, "代码片段不存在"));
        codeDependencyRepository.deleteByCodeSnippetId(id);
        versionInfoRepository.deleteBySnippetId(id);
        codeSnippetRepository.deleteById(id);
        return true;
    }

    @Override
    public CodeSnippet getCodeSnippetById(Long id) {
        return codeSnippetRepository.findById(id).orElse(null);
    }

    @Override
    public Page<CodeSnippet> listCodeSnippetByPage(Pageable pageable) {
        return codeSnippetRepository.findAll(pageable);
    }

    @Override
    public List<CodeSnippet> listCodeSnippetByLanguage(String languageType) {
        return codeSnippetRepository.findByLanguageType(languageType);
    }

    @Override
    public List<CodeSnippet> listCodeSnippetByTag(String tag) {
        return codeSnippetRepository.findByTagName(tag);
    }

    @Override
    public Boolean isFilePathExists(String filePath) {
        return codeSnippetRepository.findByFilePath(filePath).isPresent();
    }

    @Override
    public CodeSnippet getCodeSnippetByPath(String filePath) {
        return codeSnippetRepository.findByFilePath(filePath).orElse(null);
    }

    @Override
    public CodeSnippet getCodeSnippetByMd5(String fileMd5) {
        return codeSnippetRepository.findByFileMd5(fileMd5);
    }

    @Override
    public Boolean deleteByFilePath(String filePath) {
        CodeSnippet snippet = codeSnippetRepository.findByFilePath(filePath).orElse(null);
        if (snippet != null) {
            codeDependencyRepository.deleteByCodeSnippetId(snippet.getId());
            versionInfoRepository.deleteBySnippetId(snippet.getId());
            codeSnippetRepository.deleteByFilePath(filePath);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getAllFilePaths() {
        return codeSnippetRepository.findAll().stream()
                .map(CodeSnippet::getFilePath)
                .toList();
    }

    @Override
    public List<CodeDependency> listDependenciesBySnippetId(Long snippetId) {
        return codeDependencyRepository.findByCodeSnippetId(snippetId);
    }

    @Override
    public Boolean saveDependencies(Long snippetId, List<String> importList) {
        // 1. 先根据 snippetId 删除旧的依赖（防止更新代码时产生冗余数据）
        codeDependencyRepository.deleteByCodeSnippetId(snippetId);

        // 2. 转换并批量保存新的依赖
        List<CodeDependency> deps = importList.stream().map(imp -> {
            CodeDependency dep = new CodeDependency();
            dep.setCodeSnippetId(snippetId);
            dep.setDependName(imp);
            dep.setDependType("IMPORT");
            return dep;
        }).toList();

        codeDependencyRepository.saveAll(deps);
        return true;
    }
}
