package com.bank.account.adapter.out.persistence;

import com.bank.account.domain.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountEntity toEntity(Account domain) {
        return new AccountEntity(
                domain.id(),
                domain.accountNumber(),
                domain.bankCode(),
                domain.branchCode(),
                domain.accountBase(),
                domain.accountType(),
                domain.customerName(),
                domain.accountNickName(),
                domain.createdAt());
    }

    public Account toDomain(AccountEntity entity) {
        String[] parts = entity.getAccountNumber().split("-");
        return new Account(
                entity.getId(),
                entity.getBankCode(),
                entity.getBranchCode(),
                parts[2],
                parts[3],
                entity.getAccountType(),
                entity.getCustomerName(),
                entity.getAccountNickName(),
                entity.getCreatedAt());
    }
}
