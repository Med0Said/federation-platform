package com.federation.news.repository;

import com.federation.news.entity.News;
import com.federation.news.entity.NewsCategory;
import com.federation.news.entity.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {

    Optional<News> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("""
           SELECT n FROM News n
           LEFT JOIN FETCH n.author
           WHERE (:search   IS NULL
                  OR LOWER(n.title) LIKE LOWER(CONCAT('%',:search,'%')))
             AND (:status   IS NULL OR n.status   = :status)
             AND (:category IS NULL OR n.category = :category)
           """)
    Page<News> findAllFiltered(
            @Param("search")   String search,
            @Param("status")   NewsStatus status,
            @Param("category") NewsCategory category,
            Pageable pageable);

    @Modifying
    @Query("UPDATE News n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") UUID id);
}
