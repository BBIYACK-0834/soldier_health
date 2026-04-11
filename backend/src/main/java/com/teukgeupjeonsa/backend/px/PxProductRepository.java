package com.teukgeupjeonsa.backend.px;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PxProductRepository extends JpaRepository<PxProduct, Long> {
    List<PxProduct> findByIsActiveTrue();
}
