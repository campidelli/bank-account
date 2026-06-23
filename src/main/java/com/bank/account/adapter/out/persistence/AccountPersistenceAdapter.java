package com.bank.account.adapter.out.persistence;

import com.bank.account.domain.exception.DatabaseUnavailableException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.port.out.AccountPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountPersistenceAdapter implements AccountPort {

    private static final Logger log = LoggerFactory.getLogger(AccountPersistenceAdapter.class);
    private static final String CB_NAME = "database";
    private static final String ACCOUNTS_CACHE = "accounts";

    private final AccountJpaRepository jpaRepository;
    private final AccountMapper mapper;
    private final CacheManager cacheManager;

    public AccountPersistenceAdapter(AccountJpaRepository jpaRepository, AccountMapper mapper,
                                     CacheManager cacheManager) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findByCustomerAndBankAndBranchFallback")
    public List<Account> findByCustomerAndBankAndBranch(String customerName, String bankCode, String branchCode) {
        try {
            return jpaRepository.findByCustomerNameAndBankCodeAndBranchCode(customerName, bankCode, branchCode)
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        } catch (DataAccessException e) {
            log.error("Database error while querying accounts for customer '{}' bank {} branch {}",
                    customerName, bankCode, branchCode, e);
            throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "existsByBankAndBranchAndAccountBaseFallback")
    public boolean existsByBankAndBranchAndAccountBase(String bankCode, String branchCode, String accountBase) {
        try {
            return jpaRepository.existsByBankCodeAndBranchCodeAndAccountBase(bankCode, branchCode, accountBase);
        } catch (DataAccessException e) {
            log.error("Database error while checking account base uniqueness for bank {} branch {}",
                    bankCode, branchCode, e);
            throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    @Override
    @Cacheable(value = "accounts", key = "#accountNumber")
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findByAccountNumberFallback")
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try {
            return jpaRepository.findByAccountNumber(accountNumber)
                    .map(mapper::toDomain);
        } catch (DataAccessException e) {
            log.error("Database error while retrieving account '{}'", accountNumber, e);
            throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "saveFallback")
    public Account save(Account account) {
        try {
            AccountEntity entity = mapper.toEntity(account);
            AccountEntity saved = jpaRepository.save(entity);
            log.info("Saved account {} for customer '{}'", saved.getAccountNumber(), saved.getCustomerName());
            return mapper.toDomain(saved);
        } catch (DataAccessException e) {
            log.error("Database error while saving account for customer '{}'", account.customerName(), e);
            throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", e);
        }
    }

    // --- Fallbacks (circuit open or half-open call rejected) ---

    private List<Account> findByCustomerAndBankAndBranchFallback(
            String customerName, String bankCode, String branchCode, Throwable t) {
        log.warn("Circuit breaker open — findByCustomerAndBankAndBranch fallback triggered: {}", t.getMessage());
        throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", t);
    }

    private boolean existsByBankAndBranchAndAccountBaseFallback(
            String bankCode, String branchCode, String accountBase, Throwable t) {
        log.warn("Circuit breaker open — existsByBankAndBranchAndAccountBase fallback triggered: {}", t.getMessage());
        throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", t);
    }

    private Optional<Account> findByAccountNumberFallback(String accountNumber, Throwable t) {
        log.warn("Circuit breaker open — attempting cache lookup for account '{}'", accountNumber);
        Cache cache = cacheManager.getCache(ACCOUNTS_CACHE);
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(accountNumber);
            if (cached != null) {
                log.info("Serving account '{}' from cache while database is unavailable", accountNumber);
                return Optional.ofNullable((Account) cached.get());
            }
        }
        log.warn("No cached value for account '{}' — database unavailable", accountNumber);
        throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", t);
    }

    private Account saveFallback(Account account, Throwable t) {
        log.warn("Circuit breaker open — save fallback triggered: {}", t.getMessage());
        throw new DatabaseUnavailableException("Database is currently unavailable. Please try again later.", t);
    }
}
