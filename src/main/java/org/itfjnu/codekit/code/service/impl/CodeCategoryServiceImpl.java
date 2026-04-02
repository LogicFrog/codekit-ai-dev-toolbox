package org.itfjnu.codekit.code.service.impl;

import lombok.RequiredArgsConstructor;
import org.itfjnu.codekit.code.model.CodeCategory;
import org.itfjnu.codekit.code.model.CodeSnippet;
import org.itfjnu.codekit.code.repository.CodeCategoryRepository;
import org.itfjnu.codekit.code.repository.CodeSnippetRepository;
import org.itfjnu.codekit.code.service.CodeCategoryService;
import org.itfjnu.codekit.common.dto.ErrorCode;
import org.itfjnu.codekit.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CodeCategoryServiceImpl implements CodeCategoryService {

    private static final String DEFAULT_CATEGORY_NAME = "未分类";

    private final CodeCategoryRepository codeCategoryRepository;
    private final CodeSnippetRepository codeSnippetRepository;

    @Override
    public List<CodeCategory> listCategories() {
        ensureDefaultCategoryExists();
        return codeCategoryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Override
    public CodeCategory createCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称不能为空");
        }
        String normalizedName = categoryName.trim();
        codeCategoryRepository.findByCategoryName(normalizedName).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.PARAM_DUPLICATE, "分类已存在: " + normalizedName);
        });

        int nextSortOrder = codeCategoryRepository.findTopByOrderBySortOrderDescIdDesc()
                .map(category -> (category.getSortOrder() == null ? 0 : category.getSortOrder()) + 1)
                .orElse(1);

        CodeCategory category = new CodeCategory();
        category.setCategoryName(normalizedName);
        category.setSortOrder(nextSortOrder);
        return codeCategoryRepository.save(category);
    }

    @Override
    public CodeCategory renameCategory(Long categoryId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "分类名称不能为空");
        }
        CodeCategory category = getCategoryOrThrow(categoryId);
        if (DEFAULT_CATEGORY_NAME.equals(category.getCategoryName())) {
            throw new BusinessException(ErrorCode.PARAM_NOT_ALLOWED, "默认分类不允许重命名");
        }

        String normalizedName = categoryName.trim();
        codeCategoryRepository.findByCategoryName(normalizedName).ifPresent(existing -> {
            if (!existing.getId().equals(categoryId)) {
                throw new BusinessException(ErrorCode.PARAM_DUPLICATE, "分类已存在: " + normalizedName);
            }
        });

        category.setCategoryName(normalizedName);
        return codeCategoryRepository.save(category);
    }

    @Override
    public Boolean deleteCategory(Long categoryId) {
        CodeCategory category = getCategoryOrThrow(categoryId);
        if (DEFAULT_CATEGORY_NAME.equals(category.getCategoryName())) {
            throw new BusinessException(ErrorCode.PARAM_NOT_ALLOWED, "默认分类不允许删除");
        }

        CodeCategory defaultCategory = getDefaultCategory();
        List<CodeSnippet> snippets = codeSnippetRepository.findByCategory_Id(categoryId);
        for (CodeSnippet snippet : snippets) {
            snippet.setCategory(defaultCategory);
        }
        if (!snippets.isEmpty()) {
            codeSnippetRepository.saveAll(snippets);
        }

        codeCategoryRepository.delete(category);
        return true;
    }

    @Override
    public CodeCategory getCategoryById(Long categoryId) {
        return codeCategoryRepository.findById(categoryId).orElse(null);
    }

    @Override
    public CodeCategory getDefaultCategory() {
        return ensureDefaultCategoryExists();
    }

    private CodeCategory getCategoryOrThrow(Long categoryId) {
        return codeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在，ID: " + categoryId));
    }

    private CodeCategory ensureDefaultCategoryExists() {
        return codeCategoryRepository.findByCategoryName(DEFAULT_CATEGORY_NAME)
                .orElseGet(() -> {
                    CodeCategory category = new CodeCategory();
                    category.setCategoryName(DEFAULT_CATEGORY_NAME);
                    category.setSortOrder(999);
                    return codeCategoryRepository.save(category);
                });
    }
}
