package com.banking.models.services;

import com.banking.exceptions.BusinessException;
import com.banking.exceptions.DailyLimitExceededException;
import com.banking.models.dto.TransferRequest;
import com.banking.models.entities.BankAccount;
import com.banking.models.entities.TransactionHistory;
import com.banking.models.repositories.BankAccountRepository;
import com.banking.models.repositories.TransactionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    private BankAccount sourceAccount;
    private BankAccount destAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        // Arrange - Thiết lập dữ liệu chung cho các testcase
        sourceAccount = BankAccount.builder()
                .id(1L)
                .accountNumber("SRC123")
                .balance(new BigDecimal("100000000")) // Số dư: 100 triệu
                .dailyLimit(new BigDecimal("50000000")) // Hạn mức ngày: 50 triệu
                .build();

        destAccount = BankAccount.builder()
                .id(2L)
                .accountNumber("DEST456")
                .balance(new BigDecimal("10000000")) // Số dư: 10 triệu
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setToAccountNumber("DEST456");
        transferRequest.setAmount(new BigDecimal("20000000")); // Chuyển 20 triệu
    }

    @Test
    @DisplayName("Giao dịch thành công (Happy Path)")
    void testTransferMoney_Success() {
        // Arrange
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumber("DEST456")).thenReturn(Optional.of(destAccount));
        
        // Giả lập tổng tiền đã chuyển trong ngày là 10 triệu (Còn 40 triệu hạn mức)
        when(transactionHistoryRepository.sumAmountByAccountIdAndTypeAndDateBetween(
                eq(1L), eq("TRANSFER_OUT"), any(), any()))
                .thenReturn(new BigDecimal("10000000"));

        // Act
        bankAccountService.transferMoney(1L, transferRequest);

        // Assert
        assertEquals(new BigDecimal("80000000"), sourceAccount.getBalance()); // 100 - 20 = 80
        assertEquals(new BigDecimal("30000000"), destAccount.getBalance()); // 10 + 20 = 30
        
        // Kiểm tra xem đã lưu 2 bản ghi TransactionHistory chưa
        verify(transactionHistoryRepository, times(2)).save(any(TransactionHistory.class));
        verify(bankAccountRepository, times(2)).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Thất bại do vượt hạn mức trong ngày (Exceed Daily Limit)")
    void testTransferMoney_ThrowsDailyLimitExceededException() {
        // Arrange
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumber("DEST456")).thenReturn(Optional.of(destAccount));

        // Giả lập tổng tiền đã chuyển trong ngày là 40 triệu
        // Lần này chuyển thêm 20 triệu -> Tổng = 60 triệu > Hạn mức 50 triệu -> PHẢI LỖI
        when(transactionHistoryRepository.sumAmountByAccountIdAndTypeAndDateBetween(
                eq(1L), eq("TRANSFER_OUT"), any(), any()))
                .thenReturn(new BigDecimal("40000000"));

        // Act & Assert
        DailyLimitExceededException exception = assertThrows(DailyLimitExceededException.class, () -> {
            bankAccountService.transferMoney(1L, transferRequest);
        });

        assertEquals("Quý khách đã vượt hạn mức giao dịch trong ngày", exception.getMessage());
        assertEquals(429, exception.getCode());
        
        // Đảm bảo không có dòng dữ liệu nào được lưu
        verify(transactionHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thất bại do số dư không đủ (Insufficient Balance)")
    void testTransferMoney_ThrowsInsufficientBalance() {
        // Arrange
        // Đổi số tiền cần chuyển thành 120 triệu (lớn hơn số dư 100 triệu)
        transferRequest.setAmount(new BigDecimal("120000000"));
        
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumber("DEST456")).thenReturn(Optional.of(destAccount));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bankAccountService.transferMoney(1L, transferRequest);
        });

        assertEquals("Số dư không đủ để thực hiện giao dịch", exception.getMessage());
        assertEquals(400, exception.getCode());
        verify(transactionHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thất bại do không tìm thấy tài khoản nguồn")
    void testTransferMoney_ThrowsSourceAccountNotFound() {
        // Arrange
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            bankAccountService.transferMoney(1L, transferRequest);
        });

        assertEquals("Không tìm thấy tài khoản nguồn", exception.getMessage());
        assertEquals(404, exception.getCode());
    }
    
    @Test
    @DisplayName("Thành công khi tổng tiền trong ngày vừa đúng bằng hạn mức (Boundary Condition)")
    void testTransferMoney_ExactDailyLimit_Success() {
        // Arrange
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(bankAccountRepository.findByAccountNumber("DEST456")).thenReturn(Optional.of(destAccount));
        
        // Giả lập tổng tiền đã chuyển trong ngày là 30 triệu
        // Chuyển thêm 20 triệu -> Tổng = 50 triệu == Hạn mức 50 triệu -> KHÔNG LỖI
        when(transactionHistoryRepository.sumAmountByAccountIdAndTypeAndDateBetween(
                eq(1L), eq("TRANSFER_OUT"), any(), any()))
                .thenReturn(new BigDecimal("30000000"));

        // Act
        bankAccountService.transferMoney(1L, transferRequest);

        // Assert
        assertEquals(new BigDecimal("80000000"), sourceAccount.getBalance()); 
        verify(transactionHistoryRepository, times(2)).save(any(TransactionHistory.class));
    }
}
