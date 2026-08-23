package com.dailymate.core.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page == 0;
        boolean last = page >= Math.max(0, totalPages - 1);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            first,
            last,
            hasNext,
            hasPrevious
        );
    }
}
