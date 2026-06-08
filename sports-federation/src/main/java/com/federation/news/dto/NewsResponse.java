package com.federation.news.dto;

import com.federation.news.entity.NewsCategory;
import com.federation.news.entity.NewsStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsResponse {
    private UUID         id;
    private String       title;
    private String       slug;
    private String       excerpt;
    private String       content;
    private NewsCategory category;
    private NewsStatus   status;
    private String       language;
    private String       coverUrl;
    private String       coverAltText;
    private UUID         authorId;
    private String       authorName;
    private boolean      featured;
    private boolean      pinned;
    private int          viewCount;
    private Instant      publishedAt;
    private String       metaTitle;
    private String       metaDescription;
    private UUID         relatedCompetitionId;
    private String       relatedCompetitionName;
    private UUID         relatedAthleteId;
    private String       relatedAthleteName;
    private UUID         relatedClubId;
    private String       relatedClubName;
    private List<TagResponse> tags;
    private Instant      createdAt;
    private Instant      updatedAt;
}
