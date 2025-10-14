package com.backend.domain.bid.repository;

import com.backend.domain.bid.dto.ProductCurrentPriceDto;
import com.backend.domain.bid.entity.Bid;
import com.backend.domain.bid.enums.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // 현재 최고 입찰가 조회
    @Query("SELECT MAX(b.bidPrice) FROM Bid b WHERE b.product.id = :productId AND b.status = 'BIDDING'")
    Optional<Long> findHighestBidPrice(@Param("productId") Long productId);

    // 이미 입찰했는지 여부
    boolean existsByProductIdAndMemberIdAndStatus(Long productId, Long memberId, BidStatus status);

    // 입찰 개수 조회
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.product.id = :productId AND b.status = 'BIDDING'")
    Integer countProductBid(@Param("productId") Long productId);

    // 상품 입찰내역(페이징)
    @Query("SELECT b FROM Bid b WHERE b.product.id = :productId AND b.status = 'BIDDING' ORDER BY b.createDate DESC")
    Page<Bid> findAllBids(@Param("productId") Long productId, Pageable pageable);

    // 상품 입찰내역(상위 n개)
    @Query("SELECT b FROM Bid b WHERE b.product.id = :productId AND b.status = 'BIDDING' ORDER BY b.createDate DESC LIMIT :limit")
    List<Bid> findNBids(@Param("productId") Long productId, @Param("limit") Integer limit);

    // 내 입찰내역 조회
    @Query("SELECT b FROM Bid b JOIN FETCH b.product p WHERE b.member.id = :memberId AND b.status = 'BIDDING' ORDER BY b.createDate DESC")
    Page<Bid> findMyBids(@Param("memberId") Long memberId, Pageable pageable);

    // 상품들 현재 최고 입찰가 조회
    @Query("""
            SELECT b.product.id AS productId, MAX(b.bidPrice) AS currentPrice
            FROM Bid b
            WHERE b.product.id IN :productIds
            AND b.status = 'BIDDING'
            GROUP BY b.product.id
            """)
    List<ProductCurrentPriceDto> findCurrentPricesForProducts(@Param("productIds") Set<Long> productIds);

    // 내가 최고 입찰자인 상품들
    @Query("""
            SELECT b FROM Bid b
            WHERE b.member.id = :memberId
            AND b.status = 'BIDDING'
            AND b.bidPrice = (
                    SELECT MAX(b2.bidPrice)
                    FROM Bid b2
                    WHERE b2.product.id = b.product.id
                    AND b2.status = 'BIDDING'
                    )
            ORDER BY b.createDate DESC
            """)
    List<Bid> findWinningBids(@Param("memberId") Long memberId);

    // 특정 상품의 모든 입찰 내역 (입찰가 내림차순)
    @Query("SELECT b FROM Bid b JOIN FETCH b.member WHERE b.product.id = :productId AND b.status = 'BIDDING' ORDER BY b.bidPrice DESC")
    List<Bid> findAllBidsByProductOrderByPriceDesc(@Param("productId") Long productId);
}
