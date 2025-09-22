package com.backend.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder

// 페이징 정보를 담는..
public class PageableResponse {
    private final int currentPage;
    private final int pageSize;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;
    private final boolean hasPrevious;
}