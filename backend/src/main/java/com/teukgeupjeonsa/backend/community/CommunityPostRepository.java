package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findTop100ByOrderByCreatedAtDesc();
    List<CommunityPost> findTop100ByUnitOrderByCreatedAtDesc(MilitaryUnit unit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CommunityPost p where p.id = :id")
    Optional<CommunityPost> findByIdForUpdate(@Param("id") Long id);
}
