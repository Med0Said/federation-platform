package com.federation.news.service;

import com.federation.athletes.entity.Athlete;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.clubs.entity.Club;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.exception.ResourceNotFoundException;
import com.federation.common.response.PagedResponse;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.repository.CompetitionRepository;
import com.federation.news.dto.NewsRequest;
import com.federation.news.dto.NewsResponse;
import com.federation.news.dto.TagResponse;
import com.federation.news.entity.News;
import com.federation.news.entity.NewsCategory;
import com.federation.news.entity.NewsStatus;
import com.federation.news.entity.Tag;
import com.federation.news.mapper.NewsMapper;
import com.federation.news.repository.NewsRepository;
import com.federation.news.repository.TagRepository;
import com.federation.users.entity.User;
import com.federation.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository        newsRepository;
    private final TagRepository         tagRepository;
    private final UserRepository        userRepository;
    private final CompetitionRepository competitionRepository;
    private final AthleteRepository     athleteRepository;
    private final ClubRepository        clubRepository;
    private final NewsMapper            newsMapper;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<NewsResponse> findAll(String search, String status,
                                                String category, Pageable pageable) {
        NewsStatus   ns = parseEnum(NewsStatus.class, status);
        NewsCategory nc = parseEnum(NewsCategory.class, category);
        String s = blank(search) ? null : search;

        Page<News> page = newsRepository.findAllFiltered(s, ns, nc, pageable);
        return PagedResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public NewsResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public NewsResponse findBySlug(String slug) {
        News news = newsRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("News", "slug", slug));
        return toResponse(news);
    }

    @Transactional
    public NewsResponse findBySlugAndIncrementViews(String slug) {
        News news = newsRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("News", "slug", slug));
        newsRepository.incrementViewCount(news.getId());
        return toResponse(news);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findAllTags() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(newsMapper::toTagResponse)
                .collect(Collectors.toList());
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public NewsResponse create(NewsRequest request) {
        News news = newsMapper.toEntity(request);
        news.setSlug(generateUniqueSlug(request.getTitle()));
        resolveRelations(news, request);
        if (news.getStatus() == null)    news.setStatus(NewsStatus.DRAFT);
        if (news.getCategory() == null)  news.setCategory(NewsCategory.GENERAL);
        if (news.getLanguage() == null)  news.setLanguage("fr");
        if (news.getStatus() == NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
            news.setPublishedAt(Instant.now());
        }
        News saved = newsRepository.save(news);
        log.info("News article created: {} ({})", saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public NewsResponse update(UUID id, NewsRequest request) {
        News news = getOrThrow(id);
        newsMapper.updateEntity(request, news);
        resolveRelations(news, request);
        if (news.getStatus() == NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
            news.setPublishedAt(Instant.now());
        }
        News saved = newsRepository.save(news);
        log.info("News article updated: {}", id);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF')")
    public NewsResponse publish(UUID id) {
        News news = getOrThrow(id);
        news.setStatus(NewsStatus.PUBLISHED);
        if (news.getPublishedAt() == null) news.setPublishedAt(Instant.now());
        return toResponse(newsRepository.save(news));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(UUID id) {
        if (!newsRepository.existsById(id)) {
            throw new ResourceNotFoundException("News", "id", id);
        }
        newsRepository.deleteById(id);
        log.info("News article deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private News getOrThrow(UUID id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News", "id", id));
    }

    private NewsResponse toResponse(News news) {
        NewsResponse r = newsMapper.toResponse(news);
        if (news.getTags() != null) {
            r.setTags(news.getTags().stream()
                    .map(newsMapper::toTagResponse)
                    .collect(Collectors.toList()));
        }
        return r;
    }

    private void resolveRelations(News news, NewsRequest request) {
        // Author
        if (request.getAuthorId() != null) {
            User author = userRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAuthorId()));
            news.setAuthor(author);
        }
        // Related competition
        if (request.getRelatedCompetitionId() != null) {
            Competition c = competitionRepository.findById(request.getRelatedCompetitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", request.getRelatedCompetitionId()));
            news.setRelatedCompetition(c);
        }
        // Related athlete
        if (request.getRelatedAthleteId() != null) {
            Athlete a = athleteRepository.findById(request.getRelatedAthleteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Athlete", "id", request.getRelatedAthleteId()));
            news.setRelatedAthlete(a);
        }
        // Related club
        if (request.getRelatedClubId() != null) {
            Club club = clubRepository.findById(request.getRelatedClubId())
                    .orElseThrow(() -> new ResourceNotFoundException("Club", "id", request.getRelatedClubId()));
            news.setRelatedClub(club);
        }
        // Tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (UUID tagId : request.getTagIds()) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
                tags.add(tag);
            }
            news.setTags(tags);
        }
    }

    private String generateUniqueSlug(String title) {
        String base = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        String slug = base;
        int counter = 1;
        while (newsRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (blank(value)) return null;
        try { return Enum.valueOf(clazz, value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
