package com.federation.dashboard;

import com.federation.athletes.entity.AthleteStatus;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.clubs.entity.ClubStatus;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.response.ApiResponse;
import com.federation.competitions.entity.Competition;
import com.federation.competitions.entity.CompetitionStatus;
import com.federation.competitions.repository.CompetitionRepository;
import com.federation.news.entity.NewsStatus;
import com.federation.news.repository.NewsRepository;
import com.federation.registrations.entity.RegistrationStatus;
import com.federation.registrations.repository.CompetitionRegistrationRepository;
import com.federation.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Key figures for the back-office dashboard. */
@Tag(name = "Dashboard", description = "Back-office key figures")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AthleteRepository                 athleteRepository;
    private final ClubRepository                    clubRepository;
    private final CompetitionRepository             competitionRepository;
    private final CompetitionRegistrationRepository registrationRepository;
    private final NewsRepository                    newsRepository;
    private final UserRepository                    userRepository;

    @Operation(summary = "Platform statistics")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FEDERATION_STAFF','ROLE_CLUB_MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        LocalDate today = LocalDate.now();
        Specification<Competition> upcoming = (root, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("endDate"), today),
                root.get("status").in(EnumSet.complementOf(
                        EnumSet.of(CompetitionStatus.DRAFT, CompetitionStatus.CANCELLED, CompetitionStatus.COMPLETED))));
        Specification<Competition> open = (root, q, cb) ->
                cb.equal(root.get("status"), CompetitionStatus.REGISTRATION_OPEN);

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("totalAthletes",          athleteRepository.count());
        stats.put("activeAthletes",         athleteRepository.countByStatus(AthleteStatus.ACTIVE));
        stats.put("totalClubs",             clubRepository.count());
        stats.put("activeClubs",            clubRepository.countByStatus(ClubStatus.ACTIVE));
        stats.put("totalCompetitions",      competitionRepository.count());
        stats.put("upcomingCompetitions",   competitionRepository.count(upcoming));
        stats.put("openForRegistration",    competitionRepository.count(open));
        stats.put("pendingRegistrations",   registrationRepository.countByStatusIn(EnumSet.of(RegistrationStatus.PENDING)));
        stats.put("confirmedRegistrations", registrationRepository.countByStatusIn(EnumSet.of(RegistrationStatus.CONFIRMED)));
        stats.put("waitlistedRegistrations",registrationRepository.countByStatusIn(EnumSet.of(RegistrationStatus.WAITLISTED)));
        stats.put("publishedNews",          newsRepository.countByStatus(NewsStatus.PUBLISHED));
        stats.put("totalUsers",             userRepository.count());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
