package com.banking.models.services;

import com.banking.exceptions.BusinessException;
import com.banking.exceptions.DailyLimitExceededException;
import com.banking.models.dto.TransferRequest;
import com.banking.models.dto.UpdateLimitRequest;
import com.banking.models.entities.BankAccount;
import com.banking.models.entities.TransactionHistory;
import com.banking.models.repositories.BankAccountRepository;
import com.banking.models.repositories.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Transactional
    public void transferMoney(Long accountId, TransferRequest request) {
        // 1. Get source account
        BankAccount sourceAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản nguồn"));

        // 2. Get destination account
        BankAccount destinationAccount = bankAccountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản nhận"));

        BigDecimal amount = request.getAmount();

        // 3. Check balance
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(400, "Số dư không đủ để thực hiện giao dịch");
        }

        // 4. Check Daily Limit
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        BigDecimal totalTransferredToday = transactionHistoryRepository.sumAmountByAccountIdAndTypeAndDateBetween(
                accountId, "TRANSFER_OUT", startOfDay, endOfDay);

        if (totalTransferredToday.add(amount).compareTo(sourceAccount.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException("Quý khách đã vượt hạn mức giao dịch trong ngày");
        }

        // 5. Perform transfer
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));

        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(destinationAccount);

        // 6. Save transaction history for source account (TRANSFER_OUT)
        TransactionHistory outHistory = TransactionHistory.builder()
                .bankAccount(sourceAccount)
                .amount(amount)
                .type("TRANSFER_OUT")
                .build();
        transactionHistoryRepository.save(outHistory);

        // 7. Save transaction history for destination account (TRANSFER_IN)
        TransactionHistory inHistory = TransactionHistory.builder()
                .bankAccount(destinationAccount)
                .amount(amount)
                .type("TRANSFER_IN")
                .build();
        transactionHistoryRepository.save(inHistory);
    }

    @Transactional
    public void updateDailyLimit(Long accountId, UpdateLimitRequest request) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy tài khoản"));

        account.setDailyLimit(request.getNewDailyLimit());
        bankAccountRepository.save(account);
    }
}
