package com.federation.competitions.controller;

import com.federation.common.response.ApiResponse;
import com.federation.common.response.PagedResponse;
import com.federation.competitions.dto.CompetitionRequest;
import com.federation.competitions.dto.CompetitionResponse;
import com.federation.competitions.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Competitions", description = "Competition management endpoints")
@RestController
@RequestMapping("/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;

    @Operation(summary = "List competitions with optional filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CompetitionResponse>>> findAll(
            @RequestParam(required = false)              String search,
            @RequestParam(required = false)              String status,
            @RequestParam(required = false)              String from,
            @RequestParam(required = false)              String to,
            @RequestParam(defaultValue = "0")            int    page,
            @RequestParam(defaultValue = "20")           int    size,
            @RequestParam(defaultValue = "startDate,asc") String sort) {

        String[] sp = sort.split(",");
        Sort s = sp.length > 1
                ? Sort.by(Sort.Direction.fromString(sp[1]), sp[0])
                : Sort.by(Sort.Direction.ASC, sp[0]);

        return ResponseEntity.ok(ApiResponse.ok(
                competitionService.findAll(search, status, from, to,
                        PageRequest.of(page, size, s))));
    }

    @Operation(summary = "Get competition by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompetitionResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(competitionService.findById(id)));
    }

    @Operation(summary = "Get competition by slug")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CompetitionResponse>> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(competitionService.findBySlug(slug)));
    }

    @Operation(summary = "Create a new competition")
    @PostMapping
    public ResponseEntity<ApiResponse<CompetitionResponse>> create(
            @Valid @RequestBody CompetitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(competitionService.create(request)));
    }

    @Operation(summary = "Update a competition")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompetitionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CompetitionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(competitionService.update(id, request)));
    }

    @Operation(summary = "Delete a competition")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        competitionService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
