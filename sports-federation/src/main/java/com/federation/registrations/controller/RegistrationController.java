package com.federation.registrations.controller;

import com.federation.common.response.ApiResponse;
import com.federation.common.response.PagedResponse;
import com.federation.common.util.PageRequests;
import com.federation.registrations.dto.RegistrationRequest;
import com.federation.registrations.dto.RegistrationResponse;
import com.federation.registrations.dto.RegistrationStatusRequest;
import com.federation.registrations.dto.RegistrationUpdateRequest;
import com.federation.registrations.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Competition registrations.
 *
 *   POST   /competitions/{id}/registrations          register an athlete for an event
 *   GET    /competitions/{id}/registrations          list entries (staff; club managers with clubId)
 *   POST   /competitions/{id}/registrations/bibs     assign bib numbers to confirmed entries
 *   GET    /athletes/{id}/registrations              an athlete's registrations
 *   GET    /registrations/me                         the current athlete's registrations
 *   GET    /registrations/{id}                       detail
 *   PATCH  /registrations/{id}/status                confirm / waitlist / cancel / disqualify
 *   PATCH  /registrations/{id}                       staff edits (bib, seed, fee paid, notes)
 */
@Tag(name = "Registrations", description = "Athlete registrations to competition events")
@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "createdAt");

    private final RegistrationService registrationService;

    @Operation(summary = "Register an athlete for an event of the competition")
    @PostMapping("/competitions/{competitionId}/registrations")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @PathVariable UUID competitionId,
            @Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse body = registrationService.register(competitionId, request);
        String message = body.getStatus().name().equals("WAITLISTED")
                ? "The event is full — you have been placed on the waiting list (position "
                  + body.getWaitlistPosition() + ")."
                : "Registration received.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body, message));
    }

    @Operation(summary = "List registrations of a competition")
    @GetMapping("/competitions/{competitionId}/registrations")
    public ResponseEntity<ApiResponse<PagedResponse<RegistrationResponse>>> findByCompetition(
            @PathVariable UUID competitionId,
            @RequestParam(required = false) UUID   eventId,
            @RequestParam(required = false) UUID   clubId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.findByCompetition(
                competitionId, eventId, clubId, status, PageRequests.of(page, size, sort, DEFAULT_SORT))));
    }

    @Operation(summary = "Assign bib numbers to confirmed registrations without one")
    @PostMapping("/competitions/{competitionId}/registrations/bibs")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> assignBibs(@PathVariable UUID competitionId) {
        int assigned = registrationService.assignBibNumbers(competitionId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("assigned", assigned),
                assigned + " bib number(s) assigned."));
    }

    @Operation(summary = "List an athlete's registrations")
    @GetMapping("/athletes/{athleteId}/registrations")
    public ResponseEntity<ApiResponse<PagedResponse<RegistrationResponse>>> findByAthlete(
            @PathVariable UUID athleteId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.findByAthlete(
                athleteId, status, PageRequests.of(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(summary = "List the current athlete's registrations")
    @GetMapping("/registrations/me")
    public ResponseEntity<ApiResponse<PagedResponse<RegistrationResponse>>> findMine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.findMine(
                status, PageRequests.of(page, size, null, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(summary = "Get a registration")
    @GetMapping("/registrations/{id}")
    public ResponseEntity<ApiResponse<RegistrationResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.findById(id)));
    }

    @Operation(summary = "Change a registration's status (confirm, waitlist, cancel, disqualify)")
    @PatchMapping("/registrations/{id}/status")
    public ResponseEntity<ApiResponse<RegistrationResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.changeStatus(id, request)));
    }

    @Operation(summary = "Update bib, seed, payment or notes of a registration")
    @PatchMapping("/registrations/{id}")
    public ResponseEntity<ApiResponse<RegistrationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.update(id, request)));
    }
}
