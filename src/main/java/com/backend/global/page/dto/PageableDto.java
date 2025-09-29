package com.backend.global.page.dto;

public record PageableDto(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {}