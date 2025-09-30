package com.backend.domain.cash.repository;

import com.backend.domain.cash.entity.Cash;
import com.backend.domain.cash.entity.CashTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

    // 최신순 목록..
    Page<CashTransaction> findAllByCashOrderByIdDesc(Cash cash, Pageable pageable);

    // 단건 상세..
    Optional<CashTransaction> findByIdAndCash(Long id, Cash cash);
}
