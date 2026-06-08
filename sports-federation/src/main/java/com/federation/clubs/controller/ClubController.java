package com.federation.clubs.controller;

import com.federation.clubs.dto.ClubRequest;
import com.federation.clubs.dto.ClubResponse;
import com.federation.clubs.service.ClubService;
import com.federation.common.response.ApiResponse;
import com.federation.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Clubs", description = "Club management endpoints")
@RestController
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @Operation(summary = "List all clubs with optional search and filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ClubResponse>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        String[] sortParts = sort.split(",");
        Sort s = sortParts.length > 1
                ? Sort.by(Sort.Direction.fromString(sortParts[1]), sortParts[0])
                : Sort.by(Sort.Direction.ASC, sortParts[0]);

        Pageable pageable = PageRequest.of(page, size, s);
        return ResponseEntity.ok(ApiResponse.ok(clubService.findAll(search, status, pageable)));
    }

    @Operation(summary = "Get a club by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.findById(id)));
    }

    @Operation(summary = "Get a club by slug")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ClubResponse>> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.findBySlug(slug)));
    }

    @Operation(summary = "Create a new club")
    @PostMapping
    public ResponseEntity<ApiResponse<ClubResponse>> create(@Valid @RequestBody ClubRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(clubService.create(request)));
    }

    @Operation(summary = "Update a club")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClubRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(clubService.update(id, request)));
    }

    @Operation(summary = "Delete a club")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        clubService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
