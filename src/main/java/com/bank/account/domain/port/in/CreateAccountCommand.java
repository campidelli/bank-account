package com.bank.account.domain.port.in;

public record CreateAccountCommand(
        String bankCode,
        String branchCode,
        String customerName,
        String accountNickName
) {}
