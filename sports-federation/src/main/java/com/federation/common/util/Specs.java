package com.federation.common.util;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;

/**
 * Null-tolerant building blocks for Spring Data {@link Specification}s.
 *
 * Every factory returns {@code null} when its filter value is absent, and
 * {@code Specification.where(..).and(..)} ignores {@code null} — so optional
 * filters never reach SQL as {@code :param IS NULL} (which PostgreSQL cannot
 * type-infer for enum / text parameters).
 */
public final class Specs {

    private Specs() {}

    /** Case-insensitive "contains" match across one or more (dotted) attribute paths. */
    public static <T> Specification<T> search(String term, String... paths) {
        if (term == null || term.isBlank()) return null;
        String pattern = "%" + term.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return (root, query, cb) -> cb.or(Arrays.stream(paths)
                .map(p -> cb.like(cb.lower(Specs.<String>path(root, p)), pattern, '!'))
                .toArray(Predicate[]::new));
    }

    /** Equality on a (dotted) attribute path, e.g. {@code "club.id"}. */
    public static <T> Specification<T> eq(String path, Object value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.equal(path(root, path), value);
    }

    public static <T> Specification<T> notEq(String path, Object value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.notEqual(path(root, path), value);
    }

    public static <T> Specification<T> onOrAfter(String path, LocalDate value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(Specs.<LocalDate>path(root, path), value);
    }

    public static <T> Specification<T> onOrBefore(String path, LocalDate value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(Specs.<LocalDate>path(root, path), value);
    }

    @SuppressWarnings("unchecked")
    private static <Y> Expression<Y> path(From<?, ?> root, String dotted) {
        String[] parts = dotted.split("[.]");
        Path<?> p = root;
        for (String part : parts) p = p.get(part);
        return (Expression<Y>) p;
    }

    /** Lenient enum parsing for query-string filters: unknown/blank → {@code null}. */
    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }
}
