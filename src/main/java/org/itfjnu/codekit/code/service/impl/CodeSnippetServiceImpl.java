package org.itfjnu.codekit.code.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itfjnu.codekit.code.model.CodeCategory;
import org.itfjnu.codekit.code.model.CodeDependency;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeDependencyRepository;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.code.repository.VersionInfoRepository;
import org.itfjnu.codekit.code.service.CodeCategoryService;
import org.itfjnu.codekit.code.service.CodeSnippetService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CodeSnippetServiceImpl implements CodeSnippetService {

    private final CodeSnippetRepository codeSnippetRepository;
    private final CodeDependencyRepository codeDependencyRepository;
    private final VersionInfoRepository versionInfoRepository;
    private final CodeCategoryService codeCategoryService;

    @Override
    public CodeSnippet saveOrUpdateCodeSnippet(CodeSnippet snippet) {
        log.debug("保存或更新代码片段：filePath={}, id={}", snippet.getFilePath(), snippet.getId());

        if (snippet.getId() != null) {
            Optional<CodeSnippet> existingSnippet = codeSnippetRepository.findById(snippet.getId());
            if (existingSnippet.isPresent()) {
                CodeSnippet saved = codeSnippetRepository.save(applySnippetChanges(existingSnippet.get(), snippet));
                log.info("按 ID 更新代码片段成功：id={}, filePath={}", saved.getId(), saved.getFilePath());
                return saved;
            }
            throw new BusinessException(ErrorCode.CODE_NOT_FOUND, "代码片段不存在，ID: " + snippet.getId());
        }

        if (snippet.getFilePath() != null) {
            Optional<CodeSnippet> existingByPath = codeSnippetRepository.findByFilePath(snippet.getFilePath());
            if (existingByPath.isPresent()) {
                CodeSnippet saved = codeSnippetRepository.save(applySnippetChanges(existingByPath.get(), snippet));
                log.info("按路径更新代码片段成功：id={}, filePath={}", saved.getId(), saved.getFilePath());
                return saved;
            }
        }

        snippet.setCategory(resolveCategory(snippet.getCategory(), true));
        CodeSnippet saved = codeSnippetRepository.save(snippet);
        log.info("新建代码片段成功：id={}, filePath={}", saved.getId(), saved.getFilePath());
        return saved;
    }

    @Override
    public Boolean deleteCodeSnippetById(Long id) {
        codeSnippetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND, "代码片段不存在"));
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
    public Page<CodeSnippet> listCodeSnippetByCategory(Long categoryId, Pageable pageable) {
        requireCategory(categoryId);
        return codeSnippetRepository.findByCategory_Id(categoryId, pageable);
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
    public List<CodeSnippet> listCodeSnippetByCategory(Long categoryId) {
        requireCategory(categoryId);
        return codeSnippetRepository.findByCategory_Id(categoryId);
    }

    @Override
    public CodeSnippet assignCategory(Long snippetId, Long categoryId) {
        CodeSnippet snippet = codeSnippetRepository.findById(snippetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND, "代码片段不存在"));
        snippet.setCategory(requireCategory(categoryId));
        return codeSnippetRepository.save(snippet);
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
        if (snippet == null) {
            return false;
        }
        codeDependencyRepository.deleteByCodeSnippetId(snippet.getId());
        versionInfoRepository.deleteBySnippetId(snippet.getId());
        codeSnippetRepository.deleteByFilePath(filePath);
        return true;
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
        codeDependencyRepository.deleteByCodeSnippetId(snippetId);

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

    private CodeSnippet applySnippetChanges(CodeSnippet existing, CodeSnippet incoming) {
        existing.setCodeContent(incoming.getCodeContent());
        existing.setFileMd5(incoming.getFileMd5());
        existing.setFilePath(incoming.getFilePath());
        existing.setFileName(incoming.getFileName());
        existing.setLanguageType(incoming.getLanguageType());
        existing.setCategory(resolveCategory(incoming.getCategory(), existing.getCategory() == null));
        if (incoming.getPackageName() != null) {
            existing.setPackageName(incoming.getPackageName());
        }
        if (incoming.getClassName() != null) {
            existing.setClassName(incoming.getClassName());
        }
        if (incoming.getTags() != null && !incoming.getTags().isEmpty()) {
            existing.setTags(incoming.getTags());
        }
        return existing;
    }

    private CodeCategory resolveCategory(CodeCategory incomingCategory, boolean applyDefaultWhenMissing) {
        if (incomingCategory != null) {
            Long categoryId = incomingCategory.getId();
            if (categoryId == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "分类ID不能为空");
            }
            return requireCategory(categoryId);
        }
        return applyDefaultWhenMissing ? codeCategoryService.getDefaultCategory() : null;
    }

    private CodeCategory requireCategory(Long categoryId) {
        CodeCategory category = codeCategoryService.getCategoryById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在，ID: " + categoryId);
        }
        return category;
    }
}
