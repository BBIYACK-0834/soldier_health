package com.teukgeupjeonsa.backend.food;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByNameContainingOrSearchNameContaining(String name, String searchName);
    List<Food> findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(String name, String searchName, Pageable pageable);
    Optional<Food> findFirstByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCaseOrderBySourceCountDesc(String name, String searchName);
    Optional<Food> findFirstBySearchNameOrderBySourceCountDesc(String searchName);

    @Query("select f from Food f where lower(f.searchName) like lower(concat('%', :token, '%')) or lower(f.name) like lower(concat('%', :token, '%'))")
    List<Food> searchContains(String token, Pageable pageable);
}
