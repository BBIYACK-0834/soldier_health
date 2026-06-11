package com.teukgeupjeonsa.backend.food;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServingDefaultRepository extends JpaRepository<ServingDefault, Long> {
    Optional<ServingDefault> findFirstByCategory(String category);
}
