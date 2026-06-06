package com.teukgeupjeonsa.backend.food;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodAliasRepository extends JpaRepository<FoodAlias, Long> {
    List<FoodAlias> findByAliasNameContainingOrSearchNameContainingOrOriginalNameContaining(String aliasName, String searchName, String originalName);
    List<FoodAlias> findByAliasNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCase(String aliasName, String searchName, String originalName, Pageable pageable);
    Optional<FoodAlias> findFirstByAliasNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseOrderByFood_SourceCountDesc(String aliasName, String searchName, String originalName);
}
