package com.bank.account.domain.exception;

public class AccountLimitExceededException extends RuntimeException {

    public AccountLimitExceededException(String customerName) {
        super("Customer '" + customerName + "' has reached the maximum limit of 5 accounts");
    }
}
