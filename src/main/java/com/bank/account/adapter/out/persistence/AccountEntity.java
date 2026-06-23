package com.bank.account.adapter.out.persistence;

import jakarta.persistence.*;

import com.bank.account.domain.model.AccountType;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "bank_code", nullable = false, length = 2)
    private String bankCode;

    @Column(name = "branch_code", nullable = false, length = 4)
    private String branchCode;

    @Column(name = "account_base", nullable = false, length = 7)
    private String accountBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "account_nick_name", length = 30)
    private String accountNickName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AccountEntity() {}

    public AccountEntity(Long id, String accountNumber, String bankCode, String branchCode,
                         String accountBase, AccountType accountType, String customerName,
                         String accountNickName, LocalDateTime createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.bankCode = bankCode;
        this.branchCode = branchCode;
        this.accountBase = accountBase;
        this.accountType = accountType;
        this.customerName = customerName;
        this.accountNickName = accountNickName;
        this.createdAt = createdAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getBankCode() { return bankCode; }
    public String getBranchCode() { return branchCode; }
    public String getAccountBase() { return accountBase; }
    public AccountType getAccountType() { return accountType; }
    public String getCustomerName() { return customerName; }
    public String getAccountNickName() { return accountNickName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
