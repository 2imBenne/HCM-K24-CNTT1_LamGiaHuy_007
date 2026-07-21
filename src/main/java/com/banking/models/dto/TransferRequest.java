package com.banking.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Số tài khoản nhận không được để trống")
    private String toAccountNumber;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
}
