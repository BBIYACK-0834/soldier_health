package com.teukgeupjeonsa.backend.community;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class CommunityDtos {

    @Getter
    @Setter
    public static class CreatePostRequest {
        private CommunityCategory category;
        private String title;
        private String content;
        private String imageUrl;
        private String routineText;
    }

    @Getter
    @Setter
    public static class CreateCommentRequest {
        private String content;
        private String suggestedRoutineText;
    }

    @Getter
    @Builder
    public static class PostResponse {
        private Long id;
        private CommunityCategory category;
        private String title;
        private String content;
        private String imageUrl;
        private String routineText;
        private Long authorId;
        private String authorNickname;
        private Long unitId;
        private String unitName;
        private int commentCount;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class CommentResponse {
        private Long id;
        private Long postId;
        private Long authorId;
        private String authorNickname;
        private String content;
        private String suggestedRoutineText;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class PostDetailResponse {
        private PostResponse post;
        private List<CommentResponse> comments;
    }
}
