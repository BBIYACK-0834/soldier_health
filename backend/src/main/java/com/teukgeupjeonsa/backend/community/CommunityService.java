package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.unit.UserUnitSettingRepository;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;

    @Transactional(readOnly = true)
    public List<CommunityDtos.PostResponse> getPosts(Long userId, CommunityCategory category) {
        User user = getUser(userId);
        CommunityCategory safeCategory = category == null ? CommunityCategory.ALL : category;

        List<CommunityPost> posts;
        if (safeCategory == CommunityCategory.UNIT) {
            MilitaryUnit myUnit = userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                    .map(setting -> setting.getUnit())
                    .orElse(null);
            if (myUnit == null) {
                return List.of();
            }
            posts = communityPostRepository.findTop100ByCategoryAndUnitOrderByCreatedAtDesc(CommunityCategory.UNIT, myUnit);
        } else {
            posts = communityPostRepository.findTop100ByCategoryOrderByCreatedAtDesc(CommunityCategory.ALL);
        }

        return posts.stream().map(this::toPostResponse).toList();
    }

    @Transactional
    public CommunityDtos.PostResponse createPost(Long userId, CommunityDtos.CreatePostRequest request) {
        User user = getUser(userId);
        CommunityCategory category = request.getCategory() == null ? CommunityCategory.ALL : request.getCategory();

        MilitaryUnit unit = null;
        if (category == CommunityCategory.UNIT) {
            unit = userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                    .map(setting -> setting.getUnit())
                    .orElseThrow(() -> new IllegalArgumentException("부대 카테고리 글은 소속 부대 설정이 필요합니다."));
        }

        CommunityPost post = CommunityPost.builder()
                .author(user)
                .unit(unit)
                .category(category)
                .title(request.getTitle() == null ? "제목 없음" : request.getTitle().trim())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .routineText(request.getRoutineText())
                .build();

        return toPostResponse(communityPostRepository.save(post));
    }

    @Transactional(readOnly = true)
    public CommunityDtos.PostDetailResponse getPostDetail(Long postId) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        List<CommunityDtos.CommentResponse> comments = communityCommentRepository.findByPostOrderByCreatedAtAsc(post)
                .stream().map(this::toCommentResponse).toList();

        return CommunityDtos.PostDetailResponse.builder()
                .post(toPostResponse(post))
                .comments(comments)
                .build();
    }

    @Transactional
    public CommunityDtos.CommentResponse createComment(Long userId, Long postId, CommunityDtos.CreateCommentRequest request) {
        User user = getUser(userId);
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .author(user)
                .content(request.getContent())
                .suggestedRoutineText(request.getSuggestedRoutineText())
                .build();

        return toCommentResponse(communityCommentRepository.save(comment));
    }

    private CommunityDtos.PostResponse toPostResponse(CommunityPost post) {
        int commentCount = communityCommentRepository.findByPostOrderByCreatedAtAsc(post).size();

        return CommunityDtos.PostResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .routineText(post.getRoutineText())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getNickname())
                .unitId(post.getUnit() != null ? post.getUnit().getId() : null)
                .unitName(post.getUnit() != null ? post.getUnit().getUnitName() : null)
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private CommunityDtos.CommentResponse toCommentResponse(CommunityComment comment) {
        return CommunityDtos.CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getNickname())
                .content(comment.getContent())
                .suggestedRoutineText(comment.getSuggestedRoutineText())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
