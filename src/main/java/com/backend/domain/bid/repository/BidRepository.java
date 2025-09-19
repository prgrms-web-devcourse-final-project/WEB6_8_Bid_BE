package com.backend.domain.bid.repository;

import com.backend.domain.bid.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BidRepository extends JpaRepository<Bid,Long> {
    // 현재 최고 입찰가 조회
    @Query("SELECT MAX(b.bidPrice) FROM Bid b WHERE b.product.id = :productId AND b.status = 'bidding'")
    Optional<Long> findHighestBidPrice(@Param("productId") Long productId);
    // 이미 입찰했는지 여부
    @Query("SELECT COUNT(b)>0 FROM Bid b WHERE b.product.id = :productId AND b.member.id = :memberId AND b.status = 'bidding'")
    boolean existsProductBid(@Param("productId") Long productId,@Param("memberId") Long memberId,@Param("status") String status);
    // 입찰 개수 조회
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.product.id = :productId AND b.status = 'bidding'")
    Integer countProductBid(@Param("productId") Long productId);
    // 상품 입찰내역(페이징)
    @Query("SELECT b FROM Bid b WHERE b.product.id = :productId AND b.status = 'bidding' ORDER BY b.createDate DESC")
    Page<Bid> findAllBids(@Param("productId") Long productId, Pageable pageable);
    // 상품 입찰내역(상위 n개)
    @Query("SELECT b FROM Bid b WHERE b.product.id = :productId AND b.status = 'bidding' ORDER BY b.createDate DESC LIMIT:limit")
    List<Bid> findNBids(@Param("productId") Long productId, @Param("limit") Integer limit);
    // 내 입찰내역 조회
    @Query("SELECT b FROM Bid b JOIN FETCH b.product p WHERE b.member.id = :memberId AND b.status = 'bidding' ORDER BY b.createDate DESC")
    Page<Bid> findMyBids(@Param("memberId") Long memberId, Pageable pageable);
    // 상품들 현재 최고 입찰가 조회
    @Query("""
        SELECT b.product.id, MAX(b.bidPrice) 
        FROM Bid b
        WHERE b.product.id IN :productIds
        AND b.status = 'bidding'
        GROUP BY b.product.id
        """)
    List<Object[]> findCurrentPricesForProducts(@Param("productIds") Set<Long> productIds);
    // 내가 최고 입찰자인 상품들
    @Query("""
        SELECT b FROM Bid b
        WHERE b.member.id = :memberId
        AND b.status = 'bidding'
        AND b.bidPrice = (
                SELECT MAX(b2.bidPrice)
                FROM Bid b2
                WHERE b2.product.id = b.product.id
                AND b2.status = 'bidding'
                )
        ORDER BY b.createDate DESC
        """)
    List<Bid> findWinningBids(@Param("memberId") Long memberId);
}
