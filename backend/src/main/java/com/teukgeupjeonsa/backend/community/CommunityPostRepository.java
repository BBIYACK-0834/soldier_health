package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findTop100ByCategoryOrderByCreatedAtDesc(CommunityCategory category);
    List<CommunityPost> findTop100ByCategoryAndUnitOrderByCreatedAtDesc(CommunityCategory category, MilitaryUnit unit);
}
