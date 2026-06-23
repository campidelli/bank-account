package com.bank.account.adapter.in.web.dto;

import com.bank.account.domain.model.AccountType;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        String bankCode,
        String branchCode,
        AccountType accountType,
        String customerName,
        String accountNickName,
        LocalDateTime createdAt
) {}
