package com.dailymate.core.util;

import com.dailymate.core.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {}

    public static Pageable createPageable(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be less than zero");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort != null ? sort : Sort.unsorted());
    }

    public static Pageable createPageable(int page, int size) {
        return createPageable(page, size, null);
    }
}
