package com.federation.users.controller;

import com.federation.common.response.ApiResponse;
import com.federation.common.response.PagedResponse;
import com.federation.common.util.PageRequests;
import com.federation.users.dto.*;
import com.federation.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users", description = "Account administration (ADMIN only)")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users with optional search, role and status filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll(search, role, status,
                PageRequests.of(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(summary = "Get a user")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @Operation(summary = "Create a user with any role")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(userService.create(request)));
    }

    @Operation(summary = "Update a user's profile fields")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @Operation(summary = "Change a user's role (revokes their sessions)")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.changeRole(id, request.getRole()), "Role updated."));
    }

    @Operation(summary = "Activate, deactivate or suspend a user")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.changeStatus(id, request.getStatus()), "Status updated."));
    }

    @Operation(summary = "Reset a user's password to a generated temporary one (returned once)")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.resetPassword(id),
                "Password reset. Share the temporary password with the user securely."));
    }

    @Operation(summary = "Delete a user")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
