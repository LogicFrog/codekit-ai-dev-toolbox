package org.itfjnu.codekit.code.service;

import org.itfjnu.codekit.code.model.CodeCategory;

import java.util.List;

public interface CodeCategoryService {

    List<CodeCategory> listCategories();

    CodeCategory createCategory(String categoryName);

    CodeCategory renameCategory(Long categoryId, String categoryName);

    Boolean deleteCategory(Long categoryId);

    CodeCategory getCategoryById(Long categoryId);

    CodeCategory getDefaultCategory();
}
