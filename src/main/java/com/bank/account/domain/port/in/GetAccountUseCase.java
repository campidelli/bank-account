package com.bank.account.domain.port.in;

import com.bank.account.domain.model.Account;

public interface GetAccountUseCase {

    Account getAccountByAccountNumber(String accountNumber);
}
