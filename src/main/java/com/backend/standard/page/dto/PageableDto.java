package com.backend.standard.page.dto;

public record PageableDto(
        int currentPageNumber,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {}