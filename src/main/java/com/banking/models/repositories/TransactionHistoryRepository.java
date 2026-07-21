package com.banking.models.repositories;

import com.banking.models.entities.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionHistory t " +
           "WHERE t.bankAccount.id = :accountId " +
           "AND t.type = :type " +
           "AND t.transactionDate >= :startOfDay " +
           "AND t.transactionDate <= :endOfDay")
    BigDecimal sumAmountByAccountIdAndTypeAndDateBetween(
            @Param("accountId") Long accountId,
            @Param("type") String type,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
