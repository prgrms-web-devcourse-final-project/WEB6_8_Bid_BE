package com.backend.domain.bid.repository;

import com.backend.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid,Integer> {
    // 현재 최고 입찰가 조회
    @Query("SELECT MAX(b.bidPrice) FROM Bid b WHERE b.product.id = :productId AND b.status = 'bidding'")
    Optional<Long> findHighestBidPrice(@Param("productId") Integer productId);
    // 이미 입찰했는지 여부
    @Query("SELECT COUNT(b)>0 FROM Bid b WHERE b.product.id = :productId AND b.member.id = :memberId AND b.status = 'bidding'")
    boolean existsProductBid(@Param("productId") Integer productId,@Param("memberId") Integer memberId,@Param("status") String status);
}
