package com.backend.domain.cash.repository;

import com.backend.domain.cash.entity.Cash;
import com.backend.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CashRepository extends JpaRepository<Cash, Long> {

    Optional<Cash> findByMember(Member m);               // 잔액 조회..

    @Lock(LockModeType.PESSIMISTIC_WRITE)                // 동시성 잠금..
    Optional<Cash> findWithLockByMember(Member m);
}
