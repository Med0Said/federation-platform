package com.federation.news.controller;

import com.federation.common.response.ApiResponse;
import com.federation.common.response.PagedResponse;
import com.federation.news.dto.NewsRequest;
import com.federation.news.dto.NewsResponse;
import com.federation.news.dto.TagResponse;
import com.federation.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "News", description = "News article management endpoints")
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "List news articles")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NewsResponse>>> findAll(
            @RequestParam(required = false)               String search,
            @RequestParam(required = false)               String status,
            @RequestParam(required = false)               String category,
            @RequestParam(defaultValue = "0")             int    page,
            @RequestParam(defaultValue = "20")            int    size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort) {

        String[] sp = sort.split(",");
        Sort s = sp.length > 1
                ? Sort.by(Sort.Direction.fromString(sp[1]), sp[0])
                : Sort.by(Sort.Direction.DESC, "publishedAt");

        return ResponseEntity.ok(ApiResponse.ok(
                newsService.findAll(search, status, category, PageRequest.of(page, size, s))));
    }

    @Operation(summary = "Get article by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.findById(id)));
    }

    @Operation(summary = "Get published article by slug (increments view count)")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<NewsResponse>> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.findBySlugAndIncrementViews(slug)));
    }

    @Operation(summary = "List all tags")
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> findAllTags() {
        return ResponseEntity.ok(ApiResponse.ok(newsService.findAllTags()));
    }

    @Operation(summary = "Create a news article")
    @PostMapping
    public ResponseEntity<ApiResponse<NewsResponse>> create(
            @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(newsService.create(request)));
    }

    @Operation(summary = "Update a news article")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.update(id, request)));
    }

    @Operation(summary = "Publish a news article")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<NewsResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.publish(id)));
    }

    @Operation(summary = "Delete a news article")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        newsService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
