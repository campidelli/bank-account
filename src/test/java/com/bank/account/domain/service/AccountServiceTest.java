package com.bank.account.domain.service;

import com.bank.account.domain.exception.AccountLimitExceededException;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.ServiceUnavailableException;
import com.bank.account.domain.exception.DuplicateAccountNicknameException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.port.in.CreateAccountCommand;
import com.bank.account.domain.port.out.AccountCachePort;
import com.bank.account.domain.port.out.AccountPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountPort accountPort;

    @Mock
    private AccountCachePort accountCachePort;

    @InjectMocks
    private AccountService accountService;

    private static final String BANK = "03";
    private static final String BRANCH = "0473";
    private static final CreateAccountCommand COMMAND =
            new CreateAccountCommand(BANK, BRANCH, "Jane Doe", "JaneSavings");

    private static Account account(UUID id, String base, String suffix, String nickname) {
        return new Account(id, BANK, BRANCH, base, suffix, AccountType.SAVINGS, "JANE DOE", nickname, LocalDateTime.now());
    }

    @Test
    void createAccount_firstAccount_generatesSuffixZeroAndUniqueBase() {
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH)).thenReturn(List.of());
        when(accountPort.existsByBankAndBranchAndAccountBase(any(), any(), any())).thenReturn(false);
        when(accountPort.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            return new Account(UUID.randomUUID(), a.bankCode(), a.branchCode(), a.accountBase(), a.suffix(),
                    a.accountType(), a.customerName(), a.accountNickName(), LocalDateTime.now());
        });

        Account result = accountService.createAccount(COMMAND);

        assertThat(result.accountNumber()).matches("\\d{2}-\\d{4}-\\d{7}-00");
        assertThat(result.suffix()).isEqualTo("00");
        assertThat(result.customerName()).isEqualTo("JANE DOE");
    }

    @Test
    void createAccount_secondAccount_reusesBaseAndIncrementsSuffix() {
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH))
                .thenReturn(List.of(account(UUID.randomUUID(), "1234567", "00", null)));
        when(accountPort.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            return new Account(UUID.randomUUID(), a.bankCode(), a.branchCode(), a.accountBase(), a.suffix(),
                    a.accountType(), a.customerName(), a.accountNickName(), LocalDateTime.now());
        });

        Account result = accountService.createAccount(COMMAND);

        assertThat(result.accountNumber()).isEqualTo("03-0473-1234567-01");
        assertThat(result.suffix()).isEqualTo("01");
        assertThat(result.accountBase()).isEqualTo("1234567");
    }

    @Test
    void createAccount_incrementsFromHighestSuffix() {
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH))
                .thenReturn(List.of(account(UUID.randomUUID(), "1234567", "00", null), account(UUID.randomUUID(), "1234567", "01", null)));
        when(accountPort.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            return new Account(UUID.randomUUID(), a.bankCode(), a.branchCode(), a.accountBase(), a.suffix(),
                    a.accountType(), a.customerName(), a.accountNickName(), LocalDateTime.now());
        });

        Account result = accountService.createAccount(COMMAND);

        assertThat(result.accountNumber()).isEqualTo("03-0473-1234567-02");
    }

    @Test
    void createAccount_customerHas5Accounts_throwsAccountLimitExceededException() {
        List<Account> five = List.of(
                account(UUID.randomUUID(), "1234567", "00", null), account(UUID.randomUUID(), "1234567", "01", null),
                account(UUID.randomUUID(), "1234567", "02", null), account(UUID.randomUUID(), "1234567", "03", null),
                account(UUID.randomUUID(), "1234567", "04", null)
        );
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH)).thenReturn(five);

        assertThatThrownBy(() -> accountService.createAccount(COMMAND))
                .isInstanceOf(AccountLimitExceededException.class)
                .hasMessageContaining("JANE DOE");
    }

    @Test
    void createAccount_portThrowsDatabaseUnavailable_propagates() {
        when(accountPort.findByCustomerAndBankAndBranch(any(), any(), any()))
                .thenThrow(new ServiceUnavailableException("DB down", new RuntimeException()));

        assertThatThrownBy(() -> accountService.createAccount(COMMAND))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void getAccount_existingAccount_returnsAccount() {
        Account a = account(UUID.randomUUID(), "1234567", "00", null);
        when(accountPort.findByAccountNumber("03-0473-1234567-00")).thenReturn(Optional.of(a));

        Account result = accountService.getAccountByAccountNumber("03-0473-1234567-00");

        assertThat(result.accountNumber()).isEqualTo("03-0473-1234567-00");
        assertThat(result.customerName()).isEqualTo("JANE DOE");
    }

    @Test
    void getAccount_nonExistentAccount_throwsAccountNotFoundException() {
        when(accountPort.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByAccountNumber("NONEXISTENT"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("NONEXISTENT");
    }

    @Test
    void getAccount_portThrowsDatabaseUnavailable_propagates() {
        when(accountPort.findByAccountNumber(any()))
                .thenThrow(new ServiceUnavailableException("DB down", new RuntimeException()));

        assertThatThrownBy(() -> accountService.getAccountByAccountNumber("03-0473-1234567-00"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void createAccount_duplicateNickname_throwsDuplicateAccountNicknameException() {
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH))
                .thenReturn(List.of(account(UUID.randomUUID(), "1234567", "00", "JaneSavings")));

        assertThatThrownBy(() -> accountService.createAccount(COMMAND))
                .isInstanceOf(DuplicateAccountNicknameException.class)
                .hasMessageContaining("JaneSavings")
                .hasMessageContaining("JANE DOE")
                .hasMessageContaining(BANK)
                .hasMessageContaining(BRANCH);
    }

    @Test
    void createAccount_duplicateNicknameDifferentCase_throwsDuplicateAccountNicknameException() {
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH))
                .thenReturn(List.of(account(UUID.randomUUID(), "1234567", "00", "janesavings")));

        assertThatThrownBy(() -> accountService.createAccount(COMMAND))
                .isInstanceOf(DuplicateAccountNicknameException.class);
    }

    @Test
    void createAccount_nullNickname_noValidation() {
        CreateAccountCommand commandWithoutNickname = new CreateAccountCommand(BANK, BRANCH, "Jane Doe", null);
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH)).thenReturn(List.of());
        when(accountPort.existsByBankAndBranchAndAccountBase(any(), any(), any())).thenReturn(false);
        when(accountPort.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            return new Account(UUID.randomUUID(), a.bankCode(), a.branchCode(), a.accountBase(), a.suffix(),
                    a.accountType(), a.customerName(), a.accountNickName(), LocalDateTime.now());
        });

        Account result = accountService.createAccount(commandWithoutNickname);
        assertThat(result.accountNumber()).matches("\\d{2}-\\d{4}-\\d{7}-00");
    }

    @Test
    void createAccount_blankNickname_noValidation() {
        CreateAccountCommand commandWithBlankNickname = new CreateAccountCommand(BANK, BRANCH, "Jane Doe", "   ");
        when(accountPort.findByCustomerAndBankAndBranch("JANE DOE", BANK, BRANCH)).thenReturn(List.of());
        when(accountPort.existsByBankAndBranchAndAccountBase(any(), any(), any())).thenReturn(false);
        when(accountPort.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            return new Account(UUID.randomUUID(), a.bankCode(), a.branchCode(), a.accountBase(), a.suffix(),
                    a.accountType(), a.customerName(), a.accountNickName(), LocalDateTime.now());
        });

        Account result = accountService.createAccount(commandWithBlankNickname);
        assertThat(result.accountNumber()).matches("\\d{2}-\\d{4}-\\d{7}-00");
    }
}
