package com.backend.domain.payment.repository;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // 회원이 가진 결제수단 개수(첫 등록 여부 판단용)..
    long countByMember(Member member);

    // 같은 회원 내 별칭(alias) 중복 여부 확인..
    boolean existsByMemberAndAlias(Member member, String alias);

    // 회원의 '기본 결제수단' 한 개 찾기..
    Optional<PaymentMethod> findFirstByMemberAndIsDefaultTrue(Member member);


}
