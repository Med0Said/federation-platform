package com.federation.athletes.controller;

import com.federation.athletes.dto.AthleteRequest;
import com.federation.athletes.dto.AthleteResponse;
import com.federation.athletes.service.AthleteService;
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

@Tag(name = "Athletes", description = "Athlete management endpoints")
@RestController
@RequestMapping("/athletes")
@RequiredArgsConstructor
public class AthleteController {

    private final AthleteService athleteService;

    @Operation(summary = "List athletes with optional filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AthleteResponse>>> findAll(
            @RequestParam(required = false)               String search,
            @RequestParam(required = false)               String status,
            @RequestParam(required = false)               String gender,
            @RequestParam(required = false)               String category,
            @RequestParam(required = false)               UUID   clubId,
            @RequestParam(defaultValue = "0")             int    page,
            @RequestParam(defaultValue = "25")            int    size,
            @RequestParam(defaultValue = "lastName,asc")  String sort) {

        String[] sp = sort.split(",");
        Sort s = sp.length > 1
                ? Sort.by(Sort.Direction.fromString(sp[1]), sp[0])
                : Sort.by(Sort.Direction.ASC, sp[0]);

        Pageable pageable = PageRequest.of(page, size, s);
        return ResponseEntity.ok(ApiResponse.ok(
                athleteService.findAll(search, status, gender, category, clubId, pageable)));
    }

    @Operation(summary = "Get athlete by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AthleteResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(athleteService.findById(id)));
    }

    @Operation(summary = "Create a new athlete")
    @PostMapping
    public ResponseEntity<ApiResponse<AthleteResponse>> create(
            @Valid @RequestBody AthleteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(athleteService.create(request)));
    }

    @Operation(summary = "Update an athlete")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AthleteResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AthleteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(athleteService.update(id, request)));
    }

    @Operation(summary = "Delete an athlete")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        athleteService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
