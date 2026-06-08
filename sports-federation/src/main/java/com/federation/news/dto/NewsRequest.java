package com.federation.news.dto;

import com.federation.news.entity.NewsCategory;
import com.federation.news.entity.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    private String title;

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private NewsCategory category;
    private NewsStatus   status;

    @Size(max = 10)
    private String language;

    private UUID relatedCompetitionId;
    private UUID relatedAthleteId;
    private UUID relatedClubId;

    @Size(max = 500)
    private String coverUrl;

    @Size(max = 255)
    private String coverAltText;

    private UUID authorId;
    private boolean featured;
    private boolean pinned;

    @Size(max = 300)
    private String metaTitle;

    @Size(max = 500)
    private String metaDescription;

    private Set<UUID> tagIds;
}
