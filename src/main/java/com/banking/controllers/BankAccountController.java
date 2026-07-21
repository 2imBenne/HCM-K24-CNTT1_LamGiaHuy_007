package com.banking.controllers;

import com.banking.models.entities.BankAccount;
import com.banking.models.repositories.BankAccountRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banking.models.dto.TransferRequest;
import com.banking.models.dto.UpdateLimitRequest;
import com.banking.models.services.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bankAccounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountService bankAccountService;

    @GetMapping
    public ResponseEntity<com.banking.advice.ApiResponse<List<BankAccount>>> getAllBankAccounts() {
        return ResponseEntity.ok(com.banking.advice.ApiResponse.success(bankAccountRepository.findAll(),
                "Fetched all bank account successfully"));
    }

    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<com.banking.advice.ApiResponse<Void>> transferMoney(
            @PathVariable Long accountId,
            @Valid @RequestBody TransferRequest request) {
        bankAccountService.transferMoney(accountId, request);
        return ResponseEntity.ok(com.banking.advice.ApiResponse.success(null, "Chuyển tiền thành công"));
    }

    @PutMapping("/{accountId}/limit")
    public ResponseEntity<com.banking.advice.ApiResponse<Void>> updateDailyLimit(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateLimitRequest request) {
        bankAccountService.updateDailyLimit(accountId, request);
        return ResponseEntity.ok(com.banking.advice.ApiResponse.success(null, "Cập nhật hạn mức thành công"));
    }
}

