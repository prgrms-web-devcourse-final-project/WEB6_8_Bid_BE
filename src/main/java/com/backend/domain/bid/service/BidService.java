package com.backend.domain.bid.service;

import com.backend.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BidService {
    private final BidRepository bidRepository;
}
