package com.federation.users.repository;

import com.federation.users.entity.User;
import com.federation.users.entity.UserRole;
import com.federation.users.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ----------------------------------------------------------------
    // Lookups
    // ----------------------------------------------------------------

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    /** Accept both email and username in one query — used by login. */
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

    // ----------------------------------------------------------------
    // Existence checks
    // ----------------------------------------------------------------

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // ----------------------------------------------------------------
    // Filtered listing
    // ----------------------------------------------------------------

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @Query("""
           SELECT u FROM User u
           WHERE (:role   IS NULL OR u.role   = :role)
             AND (:status IS NULL OR u.status = :status)
           """)
    Page<User> findAllFiltered(
            @Param("role")   UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable);

    // ----------------------------------------------------------------
    // Mutations (avoid loading the entity just to change one field)
    // ----------------------------------------------------------------

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :timestamp WHERE u.id = :id")
    void updateLastLogin(@Param("id") UUID id, @Param("timestamp") Instant timestamp);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") UserStatus status);

    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :id")
    void updateRole(@Param("id") UUID id, @Param("role") UserRole role);

    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    void updatePassword(@Param("id") UUID id, @Param("password") String password);
}
