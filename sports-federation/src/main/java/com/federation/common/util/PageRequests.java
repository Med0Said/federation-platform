package com.federation.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a {@link Pageable} from the {@code page}, {@code size} and
 * {@code sort=property,direction} query parameters used by every list endpoint.
 * Unknown sort properties are rejected by Spring Data and mapped to 400 by
 * {@link com.federation.common.exception.GlobalExceptionHandler}.
 */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 200;

    private PageRequests() {}

    public static Pageable of(int page, int size, String sort, Sort fallback) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, parseSort(sort, fallback));
    }

    public static Sort parseSort(String sort, Sort fallback) {
        if (sort == null || sort.isBlank()) return fallback;
        String[] parts = sort.split(",");
        String property = parts[0].strip();
        if (property.isEmpty()) return fallback;
        Sort.Direction dir = parts.length > 1
                ? Sort.Direction.fromOptionalString(parts[1].strip()).orElse(Sort.Direction.ASC)
                : Sort.Direction.ASC;
        return Sort.by(dir, property);
    }
}
