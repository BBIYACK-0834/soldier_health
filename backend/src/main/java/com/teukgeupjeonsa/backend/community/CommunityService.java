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

    private static final int TITLE_MAX_LENGTH = 120;
    private static final int IMAGE_URL_MAX_LENGTH = 400;

    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final UserRepository userRepository;
    private final UserUnitSettingRepository userUnitSettingRepository;

    @Transactional(readOnly = true)
    public List<CommunityDtos.PostResponse> getPosts(Long userId, CommunityCategory category) {
        User user = getUser(userId);
        CommunityCategory safeCategory = category == null ? CommunityCategory.ALL : category;

        List<CommunityPost> posts;
        if (safeCategory == CommunityCategory.UNIT) {
            MilitaryUnit myUnit = getPrimaryUnit(user);
            if (myUnit == null) {
                return List.of();
            }
            posts = communityPostRepository.findTop100ByUnitOrderByCreatedAtDesc(myUnit);
        } else {
            posts = communityPostRepository.findTop100ByOrderByCreatedAtDesc();
        }

        return posts.stream().map((post) -> toPostResponse(post, user)).toList();
    }

    @Transactional
    public CommunityDtos.PostResponse createPost(Long userId, CommunityDtos.CreatePostRequest request) {
        User user = getUser(userId);
        MilitaryUnit unit = getPrimaryUnit(user);
        String title = trimToNull(request != null ? request.getTitle() : null);
        String content = trimToNull(request != null ? request.getContent() : null);

        if (title == null || content == null) {
            throw new IllegalArgumentException("제목과 내용을 모두 입력해주세요.");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            title = title.substring(0, TITLE_MAX_LENGTH);
        }

        String imageUrl = trimToNull(request != null ? request.getImageUrl() : null);
        if (imageUrl != null && imageUrl.length() > IMAGE_URL_MAX_LENGTH) {
            throw new IllegalArgumentException("이미지 URL은 400자 이하로 입력해주세요.");
        }

        CommunityPost post = CommunityPost.builder()
                .author(user)
                .unit(unit)
                .category(unit != null ? CommunityCategory.UNIT : CommunityCategory.ALL)
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .routineText(trimToNull(request != null ? request.getRoutineText() : null))
                .build();

        return toPostResponse(communityPostRepository.save(post), user);
    }

    @Transactional
    public CommunityDtos.PostResponse likePost(Long userId, Long postId) {
        User user = getUser(userId);
        CommunityPost post = communityPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        if (!communityPostLikeRepository.existsByPostAndUser(post, user)) {
            communityPostLikeRepository.save(CommunityPostLike.builder()
                    .post(post)
                    .user(user)
                    .build());
            post.setLikeCount(post.getLikeCount() + 1);
        }
        return toPostResponse(post, user);
    }

    @Transactional(readOnly = true)
    public CommunityDtos.PostDetailResponse getPostDetail(Long userId, Long postId) {
        User user = getUser(userId);
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        List<CommunityDtos.CommentResponse> comments = communityCommentRepository.findByPostOrderByCreatedAtAsc(post)
                .stream().map(this::toCommentResponse).toList();

        return CommunityDtos.PostDetailResponse.builder()
                .post(toPostResponse(post, user))
                .comments(comments)
                .build();
    }

    @Transactional
    public CommunityDtos.CommentResponse createComment(Long userId, Long postId, CommunityDtos.CreateCommentRequest request) {
        User user = getUser(userId);
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
        String content = trimToNull(request != null ? request.getContent() : null);
        String suggestedRoutineText = trimToNull(request != null ? request.getSuggestedRoutineText() : null);

        if (content == null && suggestedRoutineText == null) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .author(user)
                .content(content)
                .suggestedRoutineText(suggestedRoutineText)
                .build();

        return toCommentResponse(communityCommentRepository.save(comment));
    }

    private CommunityDtos.PostResponse toPostResponse(CommunityPost post, User viewer) {
        int commentCount = communityCommentRepository.findByPostOrderByCreatedAtAsc(post).size();
        boolean likedByMe = viewer != null && communityPostLikeRepository.existsByPostAndUser(post, viewer);

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
                .likeCount(post.getLikeCount())
                .likedByMe(likedByMe)
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

    private MilitaryUnit getPrimaryUnit(User user) {
        return userUnitSettingRepository.findByUserAndIsPrimaryTrue(user)
                .map(setting -> setting.getUnit())
                .orElse(null);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
