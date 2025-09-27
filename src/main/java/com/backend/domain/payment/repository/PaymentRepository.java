package com.backend.domain.payment.repository;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByMemberAndIdempotencyKey(Member member, String idempotencyKey); // 멱등 선조회
}

