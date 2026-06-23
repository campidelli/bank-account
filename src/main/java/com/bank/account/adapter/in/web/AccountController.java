package com.bank.account.adapter.in.web;

import com.bank.account.adapter.in.web.dto.AccountResponse;
import com.bank.account.adapter.in.web.dto.CreateAccountRequest;
import com.bank.account.adapter.in.web.exception.GlobalExceptionHandler;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.port.in.CreateAccountCommand;
import com.bank.account.domain.port.in.CreateAccountUseCase;
import com.bank.account.domain.port.in.GetAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Savings bank account operations")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase, GetAccountUseCase getAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
    }

    @Operation(summary = "Create a savings account",
               description = "Opens a new savings account for a customer under the given bank and branch. " +
                             "The first account generates a unique 7-digit base number with suffix 00. " +
                             "Subsequent accounts for the same customer reuse the base and increment the suffix.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Customer has reached the 5-account limit",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Database unavailable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("POST /api/v1/accounts - creating account for customer '{}' bank {} branch {}",
                request.getCustomerName(), request.getBankCode(), request.getBranchCode());
        CreateAccountCommand command = new CreateAccountCommand(
                request.getBankCode(), request.getBranchCode(),
                request.getCustomerName(), request.getAccountNickName());
        Account account = createAccountUseCase.createAccount(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @Operation(summary = "Get an account by account number",
               description = "Retrieves a savings account by its full NZ account number (BB-bbbb-AAAAAAA-SS). " +
                             "GET responses are cached in Redis for 10 minutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Database unavailable",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Full NZ account number, e.g. 03-0473-1234567-00")
            @PathVariable String accountNumber) {
        log.info("GET /api/v1/accounts/{}", accountNumber);
        Account account = getAccountUseCase.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(toResponse(account));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.id(), account.accountNumber(), account.bankCode(),
                account.branchCode(), account.accountType(), account.customerName(),
                account.accountNickName(), account.createdAt());
    }
}
