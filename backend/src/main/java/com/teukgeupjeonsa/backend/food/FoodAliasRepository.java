package com.teukgeupjeonsa.backend.food;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodAliasRepository extends JpaRepository<FoodAlias, Long> {
    List<FoodAlias> findByAliasNameContainingOrSearchNameContainingOrOriginalNameContaining(String aliasName, String searchName, String originalName);
}
