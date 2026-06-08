package com.federation.competitions.mapper;

import com.federation.competitions.dto.CompetitionRequest;
import com.federation.competitions.dto.CompetitionResponse;
import com.federation.competitions.entity.Competition;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CompetitionMapper {

    @Mapping(target = "hostClubId",   source = "hostClub.id")
    @Mapping(target = "hostClubName", source = "hostClub.name")
    @Mapping(target = "organizerId",  source = "organizer.id")
    @Mapping(target = "organizerName",
             expression = "java(c.getOrganizer() != null ? c.getOrganizer().getFullName() : null)")
    @Mapping(target = "eventCount",              ignore = true)
    @Mapping(target = "confirmedRegistrations",  ignore = true)
    CompetitionResponse toResponse(Competition c);

   /// @Mapping(target = "id",           ignore = true)
    @Mapping(target = "slug",         ignore = true)
    @Mapping(target = "hostClub",     ignore = true)
    @Mapping(target = "organizer",    ignore = true)
    @Mapping(target = "publishedAt",  ignore = true)
    @Mapping(target = "cancelledAt",  ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
   /// @Mapping(target = "createdAt",    ignore = true)
   /// @Mapping(target = "updatedAt",    ignore = true)
    Competition toEntity(CompetitionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ///@Mapping(target = "id",           ignore = true)
    @Mapping(target = "slug",         ignore = true)
    @Mapping(target = "hostClub",     ignore = true)
    @Mapping(target = "organizer",    ignore = true)
    @Mapping(target = "publishedAt",  ignore = true)
    @Mapping(target = "cancelledAt",  ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    ///@Mapping(target = "createdAt",    ignore = true)
    ///@Mapping(target = "updatedAt",    ignore = true)
    void updateEntity(CompetitionRequest request, @MappingTarget Competition competition);
}
