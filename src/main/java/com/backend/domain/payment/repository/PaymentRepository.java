package com.backend.domain.payment.repository;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 멱등 선조회..
    Optional<Payment> findByMemberAndIdempotencyKey(Member member, String idempotencyKey);

    // 내 결제 내역 목록..
    Page<Payment> findAllByMemberOrderByIdDesc(Member member, Pageable pageable);

    // 내 결제 단건 상세..
    Optional<Payment> findByIdAndMember(Long id, Member member);
}

