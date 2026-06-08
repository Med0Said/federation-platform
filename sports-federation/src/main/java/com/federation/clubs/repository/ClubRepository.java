package com.federation.clubs.repository;

import com.federation.clubs.entity.Club;
import com.federation.clubs.entity.ClubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface ClubRepository extends JpaRepository<Club, UUID> {

    Optional<Club> findBySlug(String slug);
    Optional<Club> findByLicenseNumber(String licenseNumber);
    boolean existsBySlug(String slug);
    boolean existsByLicenseNumber(String licenseNumber);
}