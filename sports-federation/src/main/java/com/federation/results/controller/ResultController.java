package com.federation.results.controller;

import com.federation.common.response.ApiResponse;
import com.federation.common.response.PagedResponse;
import com.federation.common.util.PageRequests;
import com.federation.results.dto.RankingResponse;
import com.federation.results.dto.ResultRequest;
import com.federation.results.dto.ResultResponse;
import com.federation.results.service.RankingService;
import com.federation.results.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Results", description = "Competition results and rankings")
@RestController
@RequestMapping("/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService  resultService;
    private final RankingService rankingService;

    @Operation(summary = "List results with optional filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ResultResponse>>> findAll(
            @RequestParam(required = false) UUID   competitionId,
            @RequestParam(required = false) UUID   eventId,
            @RequestParam(required = false) UUID   athleteId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        return ResponseEntity.ok(ApiResponse.ok(
                resultService.findAll(competitionId, eventId, athleteId, status,
                        PageRequests.of(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(summary = "Get results for a specific event and round (ordered by rank)")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<ResultResponse>>> findByEvent(
            @PathVariable UUID   eventId,
            @RequestParam(defaultValue = "FINAL") String round) {
        return ResponseEntity.ok(ApiResponse.ok(resultService.findByEventAndRound(eventId, round)));
    }

    @Operation(summary = "Leaderboard of an event round")
    @GetMapping("/event/{eventId}/rankings")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> rankings(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "FINAL") String round) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.findByEvent(eventId, round)));
    }

    @Operation(summary = "Recalculate the rankings of an event round")
    @PostMapping("/event/{eventId}/rankings/recalculate")
    public ResponseEntity<ApiResponse<List<RankingResponse>>> recalculate(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "FINAL") String round) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.recalculateAndGet(eventId, round),
                "Rankings recalculated."));
    }

    @Operation(summary = "Get a single result by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResultResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(resultService.findById(id)));
    }

    @Operation(summary = "Record a new result (rankings are recalculated automatically)")
    @PostMapping
    public ResponseEntity<ApiResponse<ResultResponse>> create(
            @Valid @RequestBody ResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(resultService.create(request)));
    }

    @Operation(summary = "Update a result (rankings are recalculated automatically)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResultResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ResultRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(resultService.update(id, request)));
    }

    @Operation(summary = "Delete a result (rankings are recalculated automatically)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        resultService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
