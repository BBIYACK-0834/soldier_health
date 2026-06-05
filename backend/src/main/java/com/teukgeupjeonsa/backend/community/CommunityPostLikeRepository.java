package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    boolean existsByPostAndUser(CommunityPost post, User user);
}
