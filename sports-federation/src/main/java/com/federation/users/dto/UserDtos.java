package com.federation.users.dto;

import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// ============================================================
// REQUEST DTOs
// ============================================================

/**
 * Payload for creating a new user (used by admin endpoints).
 */
class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase, one lowercase, one digit and one special character"
    )
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Role is required")
    private UserRole role;

    @Size(max = 20)
    private String phone;

    // getters / setters
    public String getEmail()                 { return email; }
    public void   setEmail(String e)         { this.email = e; }
    public String getUsername()              { return username; }
    public void   setUsername(String u)      { this.username = u; }
    public String getPassword()              { return password; }
    public void   setPassword(String p)      { this.password = p; }
    public String getFirstName()             { return firstName; }
    public void   setFirstName(String fn)    { this.firstName = fn; }
    public String getLastName()              { return lastName; }
    public void   setLastName(String ln)     { this.lastName = ln; }
    public UserRole getRole()                { return role; }
    public void   setRole(UserRole r)        { this.role = r; }
    public String getPhone()                 { return phone; }
    public void   setPhone(String p)         { this.phone = p; }
}


/**
 * Payload for updating an existing user's profile fields.
 */
class UpdateUserRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    public String getFirstName()           { return firstName; }
    public void   setFirstName(String fn)  { this.firstName = fn; }
    public String getLastName()            { return lastName; }
    public void   setLastName(String ln)   { this.lastName = ln; }
    public String getPhone()               { return phone; }
    public void   setPhone(String p)       { this.phone = p; }
    public String getAvatarUrl()           { return avatarUrl; }
    public void   setAvatarUrl(String url) { this.avatarUrl = url; }
}


/**
 * Payload for changing a user's password.
 */
class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase, one lowercase, one digit and one special character"
    )
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    public String getCurrentPassword()                { return currentPassword; }
    public void   setCurrentPassword(String p)        { this.currentPassword = p; }
    public String getNewPassword()                    { return newPassword; }
    public void   setNewPassword(String p)            { this.newPassword = p; }
    public String getConfirmPassword()                { return confirmPassword; }
    public void   setConfirmPassword(String p)        { this.confirmPassword = p; }
}


// ============================================================
// RESPONSE DTOs
// ============================================================

/**
 * Full user view returned to admin callers.
 */
class UserResponse {

    private UUID        id;
    private String      email;
    private String      username;
    private String      firstName;
    private String      lastName;
    private String      fullName;
    private UserRole    role;
    private UserStatus  status;
    private String      phone;
    private String      avatarUrl;
    private Instant     lastLogin;
    private Instant     createdAt;
    private Instant     updatedAt;

    // getters / setters (verbose but explicit — no Lombok in DTO layer to keep serialisation predictable)
    public UUID       getId()           { return id; }
    public void       setId(UUID id)    { this.id = id; }
    public String     getEmail()        { return email; }
    public void       setEmail(String e){ this.email = e; }
    public String     getUsername()     { return username; }
    public void       setUsername(String u){ this.username = u; }
    public String     getFirstName()    { return firstName; }
    public void       setFirstName(String f){ this.firstName = f; }
    public String     getLastName()     { return lastName; }
    public void       setLastName(String l){ this.lastName = l; }
    public String     getFullName()     { return fullName; }
    public void       setFullName(String fn){ this.fullName = fn; }
    public UserRole   getRole()         { return role; }
    public void       setRole(UserRole r){ this.role = r; }
    public UserStatus getStatus()       { return status; }
    public void       setStatus(UserStatus s){ this.status = s; }
    public String     getPhone()        { return phone; }
    public void       setPhone(String p){ this.phone = p; }
    public String     getAvatarUrl()    { return avatarUrl; }
    public void       setAvatarUrl(String url){ this.avatarUrl = url; }
    public Instant    getLastLogin()    { return lastLogin; }
    public void       setLastLogin(Instant ll){ this.lastLogin = ll; }
    public Instant    getCreatedAt()    { return createdAt; }
    public void       setCreatedAt(Instant ca){ this.createdAt = ca; }
    public Instant    getUpdatedAt()    { return updatedAt; }
    public void       setUpdatedAt(Instant ua){ this.updatedAt = ua; }
}


/**
 * Minimal public profile (no sensitive fields).
 */
class UserSummaryResponse {

    private UUID   id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private UserRole role;

    public UUID     getId()            { return id; }
    public void     setId(UUID id)     { this.id = id; }
    public String   getUsername()      { return username; }
    public void     setUsername(String u){ this.username = u; }
    public String   getFullName()      { return fullName; }
    public void     setFullName(String fn){ this.fullName = fn; }
    public String   getAvatarUrl()     { return avatarUrl; }
    public void     setAvatarUrl(String url){ this.avatarUrl = url; }
    public UserRole getRole()          { return role; }
    public void     setRole(UserRole r){ this.role = r; }
}
