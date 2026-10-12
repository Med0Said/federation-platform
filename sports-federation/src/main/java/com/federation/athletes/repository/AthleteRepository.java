package com.federation.athletes.repository;

import com.federation.athletes.entity.Athlete;
import com.federation.athletes.entity.AthleteCategory;
import com.federation.athletes.entity.AthleteStatus;
import com.federation.common.util.Gender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface AthleteRepository extends JpaRepository<Athlete, UUID>, JpaSpecificationExecutor<Athlete> {

    Optional<Athlete> findByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumber(String licenseNumber);
    long countByClubIdAndStatus(UUID clubId, AthleteStatus status);
    long countByStatus(AthleteStatus status);
    Optional<Athlete> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}