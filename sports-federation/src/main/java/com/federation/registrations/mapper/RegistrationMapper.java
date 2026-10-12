package com.federation.registrations.mapper;

import com.federation.registrations.dto.RegistrationResponse;
import com.federation.registrations.entity.CompetitionRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {

    @Mapping(target = "competitionId",        source = "competition.id")
    @Mapping(target = "competitionName",      source = "competition.name")
    @Mapping(target = "competitionStartDate", source = "competition.startDate")
    @Mapping(target = "competitionStatus",    source = "competition.status")
    @Mapping(target = "eventId",              source = "event.id")
    @Mapping(target = "eventName",            source = "event.name")
    @Mapping(target = "discipline",           source = "event.discipline")
    @Mapping(target = "athleteId",            source = "athlete.id")
    @Mapping(target = "athleteName",          expression = "java(r.getAthlete().getFullName())")
    @Mapping(target = "licenseNumber",        source = "athlete.licenseNumber")
    @Mapping(target = "clubId",               source = "club.id")
    @Mapping(target = "clubName",             source = "club.name")
    @Mapping(target = "registeredById",       source = "registeredBy.id")
    @Mapping(target = "registeredByName",
             expression = "java(r.getRegisteredBy() != null ? r.getRegisteredBy().getFullName() : null)")
    RegistrationResponse toResponse(CompetitionRegistration r);
}
