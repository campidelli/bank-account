package com.bank.account.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    List<AccountEntity> findByCustomerNameAndBankCodeAndBranchCode(
            String customerName, String bankCode, String branchCode);

    boolean existsByBankCodeAndBranchCodeAndAccountBase(
            String bankCode, String branchCode, String accountBase);

    Optional<AccountEntity> findByAccountNumber(String accountNumber);
}
