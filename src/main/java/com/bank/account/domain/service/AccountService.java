package com.bank.account.domain.service;

import com.bank.account.domain.exception.AccountLimitExceededException;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.DuplicateAccountNicknameException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.port.in.CreateAccountCommand;
import com.bank.account.domain.port.in.CreateAccountUseCase;
import com.bank.account.domain.port.in.GetAccountUseCase;
import com.bank.account.domain.port.out.AccountPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService implements CreateAccountUseCase, GetAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final int MAX_ACCOUNTS_PER_CUSTOMER = 5;

    private final AccountPort accountPort;

    public AccountService(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @Override
    @Transactional
    public Account createAccount(CreateAccountCommand command) {
        String customerName = command.customerName().toUpperCase();
        List<Account> existing = accountPort.findByCustomerAndBankAndBranch(
                customerName, command.bankCode(), command.branchCode());

        if (existing.size() >= MAX_ACCOUNTS_PER_CUSTOMER) {
            throw new AccountLimitExceededException(customerName);
        }

        // Validate accountNickName uniqueness per customer/bank/branch (case-insensitive)
        if (command.accountNickName() != null && !command.accountNickName().isBlank()) {
            boolean nicknameExists = existing.stream()
                    .anyMatch(acc -> command.accountNickName().equalsIgnoreCase(acc.accountNickName()));
            if (nicknameExists) {
                throw new DuplicateAccountNicknameException(command.accountNickName(), customerName,
                        command.bankCode(), command.branchCode());
            }
        }

        String base;
        String suffix;
        if (existing.isEmpty()) {
            base = generateUniqueBase(command.bankCode(), command.branchCode());
            suffix = "00";
        } else {
            Account latest = existing.stream()
                    .max(Comparator.comparing(Account::suffix))
                    .orElseThrow();
            int nextSuffix = Integer.parseInt(latest.suffix()) + 1;
            if (nextSuffix > 99) {
                throw new AccountLimitExceededException(customerName);
            }
            base = latest.accountBase();
            suffix = String.format("%02d", nextSuffix);
        }

        Account account = new Account(null, command.bankCode(), command.branchCode(),
                base, suffix, AccountType.SAVINGS, customerName, command.accountNickName(), null);

        Account saved = accountPort.save(account);
        log.info("Created account {} for customer '{}'", saved.accountNumber(), saved.customerName());
        return saved;
    }

    @Override
    public Account getAccountByAccountNumber(String accountNumber) {
        return accountPort.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private String generateUniqueBase(String bankCode, String branchCode) {
        String base;
        do {
            int n = ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
            base = String.format("%07d", n);
        } while (accountPort.existsByBankAndBranchAndAccountBase(bankCode, branchCode, base));
        return base;
    }
}
