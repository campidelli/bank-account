package com.bank.account.domain.port.in;

import com.bank.account.domain.model.Account;

public interface CreateAccountUseCase {

    Account createAccount(CreateAccountCommand command);
}
