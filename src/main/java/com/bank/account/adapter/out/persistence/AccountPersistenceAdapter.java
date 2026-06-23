package com.bank.account.adapter.out.persistence;

import com.bank.account.domain.exception.ServiceUnavailableException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.port.out.AccountPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountPersistenceAdapter implements AccountPort {

    private static final Logger log = LoggerFactory.getLogger(AccountPersistenceAdapter.class);

    private final AccountJpaRepository jpaRepository;
    private final AccountMapper mapper;

    public AccountPersistenceAdapter(AccountJpaRepository jpaRepository, AccountMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Account> findByCustomerAndBankAndBranch(String customerName, String bankCode, String branchCode) {
        try {
            return jpaRepository.findByCustomerNameAndBankCodeAndBranchCode(customerName, bankCode, branchCode)
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            log.error("Database error while querying accounts for customer '{}' bank {} branch {}",
                    customerName, bankCode, branchCode, e);
            throw new ServiceUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    @Override
    public boolean existsByBankAndBranchAndAccountBase(String bankCode, String branchCode, String accountBase) {
        try {
            return jpaRepository.existsByBankCodeAndBranchCodeAndAccountBase(bankCode, branchCode, accountBase);
        } catch (DataAccessException e) {
            log.error("Database error while checking account base uniqueness for bank {} branch {}",
                    bankCode, branchCode, e);
            throw new ServiceUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try {
            return jpaRepository.findByAccountNumber(accountNumber)
                    .map(mapper::toDomain);
        } catch (DataAccessException e) {
            log.error("Database error while retrieving account '{}'", accountNumber, e);
            throw new ServiceUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    @Override
    public Account save(Account account) {
        try {
            AccountEntity entity = mapper.toEntity(account);
            AccountEntity saved = jpaRepository.save(entity);
            log.info("Saved account {} for customer '{}'", saved.getAccountNumber(), saved.getCustomerName());
            return mapper.toDomain(saved);
        } catch (DataAccessException e) {
            log.error("Database error while saving account for customer '{}'", account.customerName(), e);
            throw new ServiceUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

}
