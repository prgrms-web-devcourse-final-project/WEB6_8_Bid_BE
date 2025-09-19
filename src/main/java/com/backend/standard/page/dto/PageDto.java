package com.backend.standard.page.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageDto<T>(
        List<T> content,
        PageableDto pageable
) {
    public static <T> PageDto<T> fromPage(Page<T> page) {
        return new PageDto<>(
            page.getContent(),
            new PageableDto(
                page.getNumber() + 1,  // 0-based를 1-based로 변환
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
            )
        );
    }
}