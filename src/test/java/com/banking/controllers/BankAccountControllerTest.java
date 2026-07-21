package com.banking.controllers;

import com.banking.advice.GlobalExceptionHandler;
import com.banking.exceptions.DailyLimitExceededException;
import com.banking.models.dto.TransferRequest;
import com.banking.models.services.BankAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BankAccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BankAccountService bankAccountService;

    @Mock
    private com.banking.models.repositories.BankAccountRepository bankAccountRepository;

    @InjectMocks
    private BankAccountController bankAccountController;


    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Khởi tạo MockMvc với GlobalExceptionHandler để test chính xác mã lỗi HTTP
        mockMvc = MockMvcBuilders.standaloneSetup(bankAccountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API Chuyển tiền thành công trả về HTTP 200 OK")
    void testTransferMoney_Success() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setToAccountNumber("DEST456");
        request.setAmount(new BigDecimal("1000000"));

        mockMvc.perform(post("/api/v1/bankAccounts/1/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chuyển tiền thành công"));
    }

    @Test
    @DisplayName("API Chuyển tiền thất bại do vượt hạn mức trả về HTTP 429 Too Many Requests")
    void testTransferMoney_DailyLimitExceeded_Returns429() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setToAccountNumber("DEST456");
        request.setAmount(new BigDecimal("60000000")); // Số tiền lớn

        // Giả lập Service ném ra lỗi DailyLimitExceededException
        doThrow(new DailyLimitExceededException("Quý khách đã vượt hạn mức giao dịch trong ngày"))
                .when(bankAccountService).transferMoney(eq(1L), any(TransferRequest.class));

        // Gọi API và assert phải trả về đúng mã 429 theo yêu cầu đề bài
        mockMvc.perform(post("/api/v1/bankAccounts/1/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("Quý khách đã vượt hạn mức giao dịch trong ngày"));
    }
}
