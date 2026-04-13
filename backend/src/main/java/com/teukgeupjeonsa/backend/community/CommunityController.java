package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import com.teukgeupjeonsa.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public ApiResponse<List<CommunityDtos.PostResponse>> getPosts(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) CommunityCategory category
    ) {
        return ApiResponse.ok(communityService.getPosts(user.getId(), category));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityDtos.PostResponse> createPost(
            @AuthenticationPrincipal User user,
            @RequestBody CommunityDtos.CreatePostRequest request
    ) {
        return ApiResponse.ok(communityService.createPost(user.getId(), request));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityDtos.PostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.ok(communityService.getPostDetail(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityDtos.CommentResponse> createComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long postId,
            @RequestBody CommunityDtos.CreateCommentRequest request
    ) {
        return ApiResponse.ok(communityService.createComment(user.getId(), postId, request));
    }
}
