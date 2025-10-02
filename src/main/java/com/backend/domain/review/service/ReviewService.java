package com.backend.domain.review.service;


import com.backend.domain.review.dto.ReviewWriteRequestDto;
import com.backend.domain.review.dto.ReviewWriteResponseDto;
import com.backend.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    public RsData<ReviewWriteResponseDto> create(String name, ReviewWriteRequestDto reviewWriteRequestDto) {


        return new RsData<>("200-1", "리뷰 작성이 완료되었습니다", responseDto);
    }
}
