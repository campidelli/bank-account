package com.bank.account.adapter.in.web;

import com.bank.account.domain.exception.AccountLimitExceededException;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.port.in.CreateAccountUseCase;
import com.bank.account.domain.port.in.GetAccountUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AccountControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private CreateAccountUseCase createAccountUseCase;

    @MockitoBean
    private GetAccountUseCase getAccountUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void createAccount_validRequest_returns201() throws Exception {
        Account account = new Account(1L, "03", "0473", "1234567", "00", AccountType.SAVINGS, "JOHN SMITH", "MyAccount", LocalDateTime.now());
        when(createAccountUseCase.createAccount(any())).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"03","branchCode":"0473","customerName":"John Smith","accountNickName":"MyAccount"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("03-0473-1234567-00"))
                .andExpect(jsonPath("$.bankCode").value("03"))
                .andExpect(jsonPath("$.branchCode").value("0473"))
                .andExpect(jsonPath("$.customerName").value("JOHN SMITH"));
    }

    @Test
    void createAccount_missingBankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchCode":"0473","customerName":"John Smith"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.bank.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[?(@.field=='bankCode')]").exists());
    }

    @Test
    void createAccount_invalidBankCodeFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"ABC","branchCode":"0473","customerName":"John Smith"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field=='bankCode')]").exists());
    }

    @Test
    void createAccount_invalidBranchCodeFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"03","branchCode":"047","customerName":"John Smith"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field=='branchCode')]").exists());
    }

    @Test
    void createAccount_missingCustomerName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"03","branchCode":"0473"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field=='customerName')]").exists());
    }

    @Test
    void createAccount_nickNameTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"03","branchCode":"0473","customerName":"John Smith","accountNickName":"abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field=='accountNickName')]").exists());
    }

    @Test
    void createAccount_accountLimitReached_returns422() throws Exception {
        when(createAccountUseCase.createAccount(any()))
                .thenThrow(new AccountLimitExceededException("John Smith"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bankCode":"03","branchCode":"0473","customerName":"John Smith"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.bank.com/problems/account-limit-exceeded"))
                .andExpect(jsonPath("$.title").value("Account Limit Exceeded"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.instance").value("/api/v1/accounts"));
    }

    @Test
    void getAccount_existingAccount_returns200() throws Exception {
        Account account = new Account(1L, "03", "0473", "1234567", "00", AccountType.SAVINGS, "JOHN SMITH", null, LocalDateTime.now());
        when(getAccountUseCase.getAccountByAccountNumber("03-0473-1234567-00")).thenReturn(account);

        mockMvc.perform(get("/api/v1/accounts/03-0473-1234567-00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("03-0473-1234567-00"))
                .andExpect(jsonPath("$.bankCode").value("03"))
                .andExpect(jsonPath("$.branchCode").value("0473"));
    }

    @Test
    void getAccount_notFound_returns404() throws Exception {
        when(getAccountUseCase.getAccountByAccountNumber("NONEXISTENT"))
                .thenThrow(new AccountNotFoundException("NONEXISTENT"));

        mockMvc.perform(get("/api/v1/accounts/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.bank.com/problems/account-not-found"))
                .andExpect(jsonPath("$.title").value("Account Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/v1/accounts/NONEXISTENT"));
    }
}
