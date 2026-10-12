package com.federation.competitions.mapper;

import com.federation.competitions.dto.CompetitionEventRequest;
import com.federation.competitions.dto.CompetitionEventResponse;
import com.federation.competitions.entity.CompetitionEvent;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CompetitionEventMapper {

    @Mapping(target = "competitionId",     source = "competition.id")
    @Mapping(target = "competitionName",   source = "competition.name")
    @Mapping(target = "registrationCount", ignore = true)
    @Mapping(target = "waitlistCount",     ignore = true)
    CompetitionEventResponse toResponse(CompetitionEvent event);

    @Mapping(target = "competition",   ignore = true)
    @Mapping(target = "lowerIsBetter", ignore = true)
    CompetitionEvent toEntity(CompetitionEventRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "competition",   ignore = true)
    @Mapping(target = "lowerIsBetter", ignore = true)
    void updateEntity(CompetitionEventRequest request, @MappingTarget CompetitionEvent event);
}
