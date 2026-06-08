package com.federation.auth.mapper;

import com.federation.auth.dto.UserProfileResponse;
import com.federation.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper — converts between {@link User} and auth-module DTOs.
 * Generated implementation is automatically registered as a Spring bean
 * because componentModel = "spring".
 */
@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserProfileResponse toProfileResponse(User user);
}
