package com.bank.account.adapter.in.web.dto;

import com.bank.account.adapter.in.web.validation.NotOffensiveNickName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;

public class CreateAccountRequest {

    @NotBlank(message = "Bank code is required")
    @Pattern(regexp = "\\d{2}", message = "Bank code must be exactly 2 digits")
    private String bankCode;

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "\\d{4}", message = "Branch code must be exactly 4 digits")
    private String branchCode;

    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName;

    @Size(min = 5, max = 30, message = "Account nick name must be between 5 and 30 characters")
    @NotOffensiveNickName
    private String accountNickName;

    public CreateAccountRequest() {}

    public CreateAccountRequest(String bankCode, String branchCode, String customerName, String accountNickName) {
        this.bankCode = bankCode;
        this.branchCode = branchCode;
        this.customerName = customerName;
        this.accountNickName = StringUtils.isBlank(accountNickName) ? null : accountNickName;
    }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getAccountNickName() { return accountNickName; }
    public void setAccountNickName(String accountNickName) {
        this.accountNickName = StringUtils.isBlank(accountNickName) ? null : accountNickName;
    }
}
