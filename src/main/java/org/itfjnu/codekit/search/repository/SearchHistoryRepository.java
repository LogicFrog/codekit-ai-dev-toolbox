package org.itfjnu.codekit.search.repository;

import org.itfjnu.codekit.search.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    
    List<SearchHistory> findTop10ByUserIdOrderBySearchTimeDesc(String userId);
    
    void deleteByUserId(String userId);
}
