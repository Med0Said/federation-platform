package com.federation.news.repository;

import com.federation.news.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlug(String slug);
    boolean existsByName(String name);
    List<Tag> findAllByOrderByNameAsc();
}
