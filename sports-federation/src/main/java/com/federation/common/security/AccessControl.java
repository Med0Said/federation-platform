package com.federation.common.security;

import com.federation.athletes.entity.Athlete;
import com.federation.athletes.repository.AthleteRepository;
import com.federation.clubs.repository.ClubRepository;
import com.federation.common.exception.ForbiddenException;
import com.federation.common.util.SecurityUtils;
import com.federation.users.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Row-level ("ownership") authorisation rules shared by services.
 * Role-level checks stay in {@code @PreAuthorize}; this class answers
 * "may <em>this</em> user act on <em>that</em> club / athlete?".
 */
@Component
@RequiredArgsConstructor
public class AccessControl {

    private final ClubRepository    clubRepository;
    private final AthleteRepository athleteRepository;

    /** Admin or federation staff — full access to every record. */
    public boolean isStaff() {
        return SecurityUtils.hasAnyAuthority(UserRole.ADMIN, UserRole.FEDERATION_STAFF);
    }

    public boolean isClubManager() {
        return SecurityUtils.hasAnyAuthority(UserRole.CLUB_MANAGER);
    }

    public boolean isAthlete() {
        return SecurityUtils.hasAnyAuthority(UserRole.ATHLETE);
    }

    /** True when the current user is the designated manager of the club. */
    public boolean managesClub(UUID clubId) {
        if (clubId == null) return false;
        return SecurityUtils.currentUserId()
                .map(uid -> clubRepository.existsByIdAndManagerId(clubId, uid))
                .orElse(false);
    }

    /** The athlete record linked to the current user's account, if any. */
    public Optional<Athlete> currentAthlete() {
        return SecurityUtils.currentUserId().flatMap(athleteRepository::findByUserId);
    }

    /** Staff, the athlete themself, or the manager of the athlete's club. */
    public boolean canAccessAthlete(Athlete athlete) {
        if (isStaff()) return true;
        UUID uid = SecurityUtils.currentUserId().orElse(null);
        if (uid == null) return false;
        if (athlete.getUser() != null && uid.equals(athlete.getUser().getId())) return true;
        return athlete.getClub() != null && managesClub(athlete.getClub().getId());
    }

    public void requireStaffOrClubManagerOf(UUID clubId, String action) {
        if (isStaff()) return;
        if (isClubManager() && managesClub(clubId)) return;
        throw new ForbiddenException("You may only " + action + " for clubs you manage.");
    }

    public void requireAthleteAccess(Athlete athlete) {
        if (!canAccessAthlete(athlete)) {
            throw new ForbiddenException("You do not have access to this athlete.");
        }
    }
}
