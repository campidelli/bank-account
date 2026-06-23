package com.bank.account.domain.exception;

public class DuplicateAccountNicknameException extends RuntimeException {
    public DuplicateAccountNicknameException(String accountNickName, String customerName, String bankCode, String branchCode) {
        super("Account nickname '%s' already exists for customer '%s' at bank %s branch %s".formatted(
                accountNickName, customerName, bankCode, branchCode));
    }
}
