package com.federation.athletes.mapper;

import com.federation.athletes.dto.AthleteRequest;
import com.federation.athletes.dto.AthleteResponse;
import com.federation.athletes.entity.Athlete;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring")
public interface AthleteMapper {

    @Mapping(target = "fullName",  expression = "java(athlete.getFullName())")
    @Mapping(target = "age",       expression = "java(calculateAge(athlete.getDateOfBirth()))")
    @Mapping(target = "clubId",    source = "club.id")
    @Mapping(target = "clubName",  source = "club.name")
    @Mapping(target = "userId",    source = "user.id")
    AthleteResponse toResponse(Athlete athlete);

    ///@Mapping(target = "id",        ignore = true)
    @Mapping(target = "club",      ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "category",  ignore = true)
    ///@Mapping(target = "createdAt", ignore = true)
    ///@Mapping(target = "updatedAt", ignore = true)
    Athlete toEntity(AthleteRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ///@Mapping(target = "id",        ignore = true)
    @Mapping(target = "club",      ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "category",  ignore = true)
    ///@Mapping(target = "createdAt", ignore = true)
    ///@Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AthleteRequest request, @MappingTarget Athlete athlete);

    default int calculateAge(LocalDate dob) {
        if (dob == null) return 0;
        return Period.between(dob, LocalDate.now()).getYears();
    }
}
