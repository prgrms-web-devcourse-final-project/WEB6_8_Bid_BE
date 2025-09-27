package com.backend.domain.payment.repository;

import com.backend.domain.member.entity.Member;
import com.backend.domain.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // 회원이 가진 결제수단 개수(첫 등록 여부 판단용)..
    long countByMemberAndDeletedFalse(Member member);

    // 같은 회원 내 별칭(alias) 중복 여부 확인..
    boolean existsByMemberAndAliasAndDeletedFalse(Member member, String alias);

    // 나 자신은 빼고 동일 별칭을 쓰는 다른 수단이 있나?(현재 수정 대상 id 제외)
    boolean existsByMemberAndAliasAndIdNotAndDeletedFalse(Member member, String alias, Long id);

    // 회원의 기본 수단 찾기..
    Optional<PaymentMethod> findFirstByMemberAndIsDefaultTrueAndDeletedFalse(Member member);

    // 결제 수단 전체 조회(기본 수단 먼저, 그다음 최신 생성 순)..
    List<PaymentMethod> findAllByMemberAndDeletedFalseOrderByIsDefaultDescCreateDateDesc(Member member);

    // 결제 수단 단건 조회..
    Optional<PaymentMethod> findByIdAndMemberAndDeletedFalse(Long id, Member member);

    // 기본 수단을 삭제했을 때 가장 최근에 만든 다른 결제수단을 자동으로 기본으로 승계..
    Optional<PaymentMethod> findFirstByMemberAndDeletedFalseOrderByCreateDateDesc(Member member);
}
