package com.federation.competitions.controller;

import com.federation.common.response.ApiResponse;
import com.federation.competitions.dto.CompetitionEventRequest;
import com.federation.competitions.dto.CompetitionEventResponse;
import com.federation.competitions.service.CompetitionEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Competition Events", description = "Discipline events within a competition")
@RestController
@RequestMapping("/competitions/{competitionId}/events")
@RequiredArgsConstructor
public class CompetitionEventController {

    private final CompetitionEventService eventService;

    @Operation(summary = "List the events of a competition")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CompetitionEventResponse>>> findAll(@PathVariable UUID competitionId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCompetition(competitionId)));
    }

    @Operation(summary = "Get one event")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<CompetitionEventResponse>> findById(
            @PathVariable UUID competitionId, @PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findById(competitionId, eventId)));
    }

    @Operation(summary = "Create an event")
    @PostMapping
    public ResponseEntity<ApiResponse<CompetitionEventResponse>> create(
            @PathVariable UUID competitionId,
            @Valid @RequestBody CompetitionEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(eventService.create(competitionId, request)));
    }

    @Operation(summary = "Update an event")
    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponse<CompetitionEventResponse>> update(
            @PathVariable UUID competitionId, @PathVariable UUID eventId,
            @Valid @RequestBody CompetitionEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.update(competitionId, eventId, request)));
    }

    @Operation(summary = "Delete an event (only when it has no registrations or results)")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID competitionId, @PathVariable UUID eventId) {
        eventService.delete(competitionId, eventId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
