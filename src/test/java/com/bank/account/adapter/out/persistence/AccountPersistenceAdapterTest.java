package com.bank.account.adapter.out.persistence;

import com.bank.account.domain.exception.ServiceUnavailableException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPersistenceAdapterTest {

    @Mock
    private AccountJpaRepository jpaRepository;

    @Mock
    private AccountMapper mapper;

    @InjectMocks
    private AccountPersistenceAdapter adapter;

    private static final String BANK = "03";
    private static final String BRANCH = "0473";
    private static final String BASE = "1234567";
    private static final String SUFFIX = "00";
    private static final String ACCOUNT_NUMBER = "03-0473-1234567-00";

    private AccountEntity entity() {
        return new AccountEntity(1L, ACCOUNT_NUMBER, BANK, BRANCH, BASE, AccountType.SAVINGS, "Alice", null, null);
    }

    private Account domain() {
        return new Account(1L, BANK, BRANCH, BASE, SUFFIX, AccountType.SAVINGS, "Alice", null, null);
    }

    @Test
    void findByCustomerAndBankAndBranch_returnsMappedAccounts() {
        AccountEntity e = entity();
        Account d = domain();
        when(jpaRepository.findByCustomerNameAndBankCodeAndBranchCode("Alice", BANK, BRANCH))
                .thenReturn(List.of(e));
        when(mapper.toDomain(e)).thenReturn(d);

        List<Account> result = adapter.findByCustomerAndBankAndBranch("Alice", BANK, BRANCH);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).accountNumber()).isEqualTo(ACCOUNT_NUMBER);
    }

    @Test
    void findByCustomerAndBankAndBranch_databaseDown_throwsDatabaseUnavailableException() {
        when(jpaRepository.findByCustomerNameAndBankCodeAndBranchCode(any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> adapter.findByCustomerAndBankAndBranch("Alice", BANK, BRANCH))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void existsByBankAndBranchAndAccountBase_returnsTrue() {
        when(jpaRepository.existsByBankCodeAndBranchCodeAndAccountBase(BANK, BRANCH, BASE)).thenReturn(true);
        assertThat(adapter.existsByBankAndBranchAndAccountBase(BANK, BRANCH, BASE)).isTrue();
    }

    @Test
    void existsByBankAndBranchAndAccountBase_databaseDown_throwsDatabaseUnavailableException() {
        when(jpaRepository.existsByBankCodeAndBranchCodeAndAccountBase(any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> adapter.existsByBankAndBranchAndAccountBase(BANK, BRANCH, BASE))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void findByAccountNumber_found_returnsDomainAccount() {
        AccountEntity e = entity();
        Account d = domain();
        when(jpaRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(e));
        when(mapper.toDomain(e)).thenReturn(d);

        assertThat(adapter.findByAccountNumber(ACCOUNT_NUMBER)).contains(d);
    }

    @Test
    void findByAccountNumber_notFound_returnsEmpty() {
        when(jpaRepository.findByAccountNumber("MISSING")).thenReturn(Optional.empty());
        assertThat(adapter.findByAccountNumber("MISSING")).isEmpty();
    }

    @Test
    void findByAccountNumber_databaseDown_throwsDatabaseUnavailableException() {
        when(jpaRepository.findByAccountNumber(any()))
                .thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> adapter.findByAccountNumber(ACCOUNT_NUMBER))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void save_success_returnsDomainAccount() {
        Account input = new Account(null, BANK, BRANCH, BASE, SUFFIX, AccountType.SAVINGS, "Bob", null, null);
        AccountEntity e = entity();
        LocalDateTime now = LocalDateTime.now();
        AccountEntity saved = new AccountEntity(1L, ACCOUNT_NUMBER, BANK, BRANCH, BASE, AccountType.SAVINGS, "Bob", null, now);
        Account savedDomain = new Account(1L, BANK, BRANCH, BASE, SUFFIX, AccountType.SAVINGS, "Bob", null, now);

        when(mapper.toEntity(input)).thenReturn(e);
        when(jpaRepository.save(e)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(savedDomain);

        Account result = adapter.save(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.accountNumber()).isEqualTo(ACCOUNT_NUMBER);
    }

    @Test
    void save_databaseDown_throwsDatabaseUnavailableException() {
        Account input = new Account(null, BANK, BRANCH, BASE, SUFFIX, AccountType.SAVINGS, "Bob", null, null);
        when(mapper.toEntity(input)).thenReturn(entity());
        when(jpaRepository.save(any())).thenThrow(new DataAccessResourceFailureException("down"));

        assertThatThrownBy(() -> adapter.save(input))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
