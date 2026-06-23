package com.bank.account.domain.model;

import java.time.LocalDateTime;

public record Account(
        Long id,
        String bankCode,
        String branchCode,
        String accountBase,
        String suffix,
        AccountType accountType,
        String customerName,
        String accountNickName,
        LocalDateTime createdAt
) {
    public String accountNumber() {
        return bankCode + "-" + branchCode + "-" + accountBase + "-" + suffix;
    }
}
