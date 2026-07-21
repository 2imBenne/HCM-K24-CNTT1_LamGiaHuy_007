package com.banking.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLimitRequest {
    @NotNull(message = "Hạn mức mới không được để trống")
    @DecimalMin(value = "0", message = "Hạn mức phải lớn hơn hoặc bằng 0")
    private BigDecimal newDailyLimit;
}
