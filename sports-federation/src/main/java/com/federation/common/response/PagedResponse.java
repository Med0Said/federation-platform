package com.federation.common.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps Spring's Page object in a clean serialisable envelope.
 */
@Getter
public class PagedResponse<T> {

    private final List<T> content;
    private final int     page;
    private final int     size;
    private final long    totalElements;
    private final int     totalPages;
    private final boolean last;
    private final boolean first;

    private PagedResponse(List<T> content, int page, int size,
                          long totalElements, int totalPages,
                          boolean last, boolean first) {
        this.content       = content;
        this.page          = page;
        this.size          = size;
        this.totalElements = totalElements;
        this.totalPages    = totalPages;
        this.last          = last;
        this.first         = first;
    }

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }

    /** Use when items are already mapped (e.g. Page<Entity> → PagedResponse<DTO>) */
    public static <T> PagedResponse<T> of(Page<?> sourcePage, List<T> mappedContent) {
        return new PagedResponse<>(
                mappedContent,
                sourcePage.getNumber(),
                sourcePage.getSize(),
                sourcePage.getTotalElements(),
                sourcePage.getTotalPages(),
                sourcePage.isLast(),
                sourcePage.isFirst()
        );
    }
}
