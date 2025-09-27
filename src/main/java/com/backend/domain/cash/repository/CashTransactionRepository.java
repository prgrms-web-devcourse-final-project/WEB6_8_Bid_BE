package com.backend.domain.cash.repository;

import com.backend.domain.cash.entity.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

}
