package org.itfjnu.codekit.code.repository;

import org.itfjnu.codekit.code.model.CodeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeCategoryRepository extends JpaRepository<CodeCategory, Long> {

    Optional<CodeCategory> findByCategoryName(String categoryName);

    List<CodeCategory> findAllByOrderBySortOrderAscIdAsc();

    Optional<CodeCategory> findTopByOrderBySortOrderDescIdDesc();
}
