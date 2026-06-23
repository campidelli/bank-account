package com.bank.account.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.bank.account.domain.model.AccountType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountJpaRepositoryTest {

    @Autowired
    private AccountJpaRepository accountRepository;

    private static final String BANK = "03";
    private static final String BRANCH = "0473";
    private static final String BASE = "1234567";

    private AccountEntity newEntity(String accountNumber, String base, String customerName) {
        return new AccountEntity(null, accountNumber, BANK, BRANCH, base, AccountType.SAVINGS, customerName, null, null);
    }

    @Test
    void findByAccountNumber_existingAccount_returnsEntity() {
        accountRepository.save(newEntity("03-0473-9871001-00", "9871001", "Bob Jones"));

        Optional<AccountEntity> found = accountRepository.findByAccountNumber("03-0473-9871001-00");

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("Bob Jones");
    }

    @Test
    void findByAccountNumber_nonExistentAccount_returnsEmpty() {
        assertThat(accountRepository.findByAccountNumber("99-9999-9999999-99")).isEmpty();
    }

    @Test
    void findByCustomerNameAndBankCodeAndBranchCode_returnsMatchingAccounts() {
        accountRepository.save(newEntity("03-0473-7651001-00", "7651001", "FindAlice"));
        accountRepository.save(newEntity("03-0473-7651001-01", "7651001", "FindAlice"));
        accountRepository.save(newEntity("03-0473-7652001-00", "7652001", "FindBob"));

        assertThat(accountRepository.findByCustomerNameAndBankCodeAndBranchCode("FindAlice", BANK, BRANCH))
                .hasSize(2);
        assertThat(accountRepository.findByCustomerNameAndBankCodeAndBranchCode("FindBob", BANK, BRANCH))
                .hasSize(1);
        assertThat(accountRepository.findByCustomerNameAndBankCodeAndBranchCode("FindCharlie", BANK, BRANCH))
                .isEmpty();
    }

    @Test
    void existsByBankCodeAndBranchCodeAndAccountBase_returnsCorrectly() {
        accountRepository.save(newEntity("03-0473-5550001-00", "5550001", "ExistsTest"));

        assertThat(accountRepository.existsByBankCodeAndBranchCodeAndAccountBase(BANK, BRANCH, "5550001")).isTrue();
        assertThat(accountRepository.existsByBankCodeAndBranchCodeAndAccountBase(BANK, BRANCH, "0000000")).isFalse();
    }

    @Test
    void save_setsCreatedAtAutomatically() {
        AccountEntity saved = accountRepository.save(newEntity("03-0473-3330001-00", "3330001", "Carol"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getBankCode()).isEqualTo(BANK);
        assertThat(saved.getBranchCode()).isEqualTo(BRANCH);
        assertThat(saved.getAccountBase()).isEqualTo("3330001");
    }
}
