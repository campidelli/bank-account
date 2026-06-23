package com.bank.account.domain.port.out;

import com.bank.account.domain.model.Account;

import java.util.Optional;

public interface AccountCachePort {

    Optional<Account> get(String accountNumber);
    void put(String accountNumber, Account account);
    void evict(String accountNumber);
    void clear();
}