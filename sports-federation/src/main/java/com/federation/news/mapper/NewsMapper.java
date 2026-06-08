package com.federation.news.mapper;

import com.federation.news.dto.NewsRequest;
import com.federation.news.dto.NewsResponse;
import com.federation.news.dto.TagResponse;
import com.federation.news.entity.News;
import com.federation.news.entity.Tag;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NewsMapper {

    @Mapping(target = "authorId",   source = "author.id")
    @Mapping(target = "authorName",
             expression = "java(n.getAuthor() != null ? n.getAuthor().getFullName() : null)")
    @Mapping(target = "relatedCompetitionId",   source = "relatedCompetition.id")
    @Mapping(target = "relatedCompetitionName", source = "relatedCompetition.name")
    @Mapping(target = "relatedAthleteId",        source = "relatedAthlete.id")
    @Mapping(target = "relatedAthleteName",
             expression = "java(n.getRelatedAthlete() != null ? n.getRelatedAthlete().getFullName() : null)")
    @Mapping(target = "relatedClubId",   source = "relatedClub.id")
    @Mapping(target = "relatedClubName", source = "relatedClub.name")
    NewsResponse toResponse(News n);

    TagResponse toTagResponse(Tag tag);

    ///@Mapping(target = "id",                 ignore = true)
    @Mapping(target = "slug",               ignore = true)
    @Mapping(target = "author",             ignore = true)
    @Mapping(target = "reviewedBy",         ignore = true)
    @Mapping(target = "reviewedAt",         ignore = true)
    @Mapping(target = "publishedAt",        ignore = true)
    @Mapping(target = "archivedAt",         ignore = true)
    @Mapping(target = "viewCount",          ignore = true)
    @Mapping(target = "tags",               ignore = true)
    @Mapping(target = "relatedCompetition", ignore = true)
    @Mapping(target = "relatedAthlete",     ignore = true)
    @Mapping(target = "relatedClub",        ignore = true)
    ///@Mapping(target = "createdAt",          ignore = true)
    ///@Mapping(target = "updatedAt",          ignore = true)
    News toEntity(NewsRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ///@Mapping(target = "id",                 ignore = true)
    @Mapping(target = "slug",               ignore = true)
    @Mapping(target = "author",             ignore = true)
    @Mapping(target = "reviewedBy",         ignore = true)
    @Mapping(target = "reviewedAt",         ignore = true)
    @Mapping(target = "publishedAt",        ignore = true)
    @Mapping(target = "archivedAt",         ignore = true)
    @Mapping(target = "viewCount",          ignore = true)
    @Mapping(target = "tags",               ignore = true)
    @Mapping(target = "relatedCompetition", ignore = true)
    @Mapping(target = "relatedAthlete",     ignore = true)
    @Mapping(target = "relatedClub",        ignore = true)
    ///@Mapping(target = "createdAt",          ignore = true)
    ///@Mapping(target = "updatedAt",          ignore = true)
    void updateEntity(NewsRequest request, @MappingTarget News news);
}
