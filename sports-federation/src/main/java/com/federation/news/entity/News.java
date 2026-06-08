package com.federation.news.entity;

import com.federation.athletes.entity.Athlete;
import com.federation.clubs.entity.Club;
import com.federation.common.util.BaseEntity;
import com.federation.competitions.entity.Competition;
import com.federation.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "news")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class News extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, unique = true, length = 320)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NewsCategory category = NewsCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private NewsStatus status = NewsStatus.DRAFT;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String language = "fr";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_competition_id")
    private Competition relatedCompetition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_athlete_id")
    private Athlete relatedAthlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_club_id")
    private Club relatedClub;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "cover_alt_text", length = 255)
    private String coverAltText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "meta_title", length = 300)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "news_tags",
        joinColumns = @JoinColumn(name = "news_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}
