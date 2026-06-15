package com.teukgeupjeonsa.backend.food;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FoodAliasRepository extends JpaRepository<FoodAlias, Long> {
    List<FoodAlias> findByAliasNameContainingOrSearchNameContainingOrOriginalNameContaining(String aliasName, String searchName, String originalName);
    List<FoodAlias> findByAliasNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCase(String aliasName, String searchName, String originalName, Pageable pageable);
    Optional<FoodAlias> findFirstByAliasNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseOrderByFood_SourceCountDesc(String aliasName, String searchName, String originalName);
    Optional<FoodAlias> findFirstBySearchNameOrderByFood_SourceCountDesc(String searchName);
    List<FoodAlias> findBySearchName(String searchName);
    long countByFood_Id(Long foodId);

    @Query("select a from FoodAlias a join fetch a.food f where lower(a.searchName) like lower(concat('%', :token, '%')) or lower(a.aliasName) like lower(concat('%', :token, '%')) or lower(a.originalName) like lower(concat('%', :token, '%'))")
    List<FoodAlias> searchContains(String token, Pageable pageable);
}
