package com.bank.account.domain.port.out;

import com.bank.account.domain.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountPort {

    List<Account> findByCustomerAndBankAndBranch(String customerName, String bankCode, String branchCode);

    boolean existsByBankAndBranchAndAccountBase(String bankCode, String branchCode, String accountBase);

    Optional<Account> findByAccountNumber(String accountNumber);

    Account save(Account account);
}
