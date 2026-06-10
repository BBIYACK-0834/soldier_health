package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {
    boolean existsByPostAndUser(CommunityPost post, User user);

    Optional<CommunityPostLike> findByPostAndUser(CommunityPost post, User user);
}
