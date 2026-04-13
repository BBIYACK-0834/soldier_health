package com.teukgeupjeonsa.backend.community;

import com.teukgeupjeonsa.backend.unit.MilitaryUnit;
import com.teukgeupjeonsa.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private MilitaryUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityCategory category;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    private String content;

    @Column(length = 400)
    private String imageUrl;

    @Lob
    private String routineText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
