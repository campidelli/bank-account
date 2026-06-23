package com.bank.account.adapter.in.web.exception;

import com.bank.account.domain.exception.AccountLimitExceededException;
import com.bank.account.domain.exception.AccountNotFoundException;
import com.bank.account.domain.exception.DatabaseUnavailableException;
import com.bank.account.domain.exception.DuplicateAccountNicknameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://api.bank.com/problems/";

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "account-not-found"));
        problem.setTitle("Account Not Found");
        return problem;
    }

    @ExceptionHandler(AccountLimitExceededException.class)
    public ProblemDetail handleAccountLimitExceeded(AccountLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "account-limit-exceeded"));
        problem.setTitle("Account Limit Exceeded");
        return problem;
    }

    @ExceptionHandler(DuplicateAccountNicknameException.class)
    public ProblemDetail handleDuplicateAccountNickname(DuplicateAccountNicknameException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "duplicate-account-nickname"));
        problem.setTitle("Duplicate Account Nickname");
        return problem;
    }

    @ExceptionHandler(DatabaseUnavailableException.class)
    public ProblemDetail handleDatabaseUnavailable(DatabaseUnavailableException ex) {
        log.error("Database unavailable", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + "database-unavailable"));
        problem.setTitle("Database Unavailable");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setTitle("Internal Server Error");
        return problem;
    }

    /**
     * Overrides the default Spring handler to append per-field violations as an
     * "errors" extension property, keeping the standard RFC 9457 fields intact.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setType(URI.create(TYPE_BASE + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setDetail("One or more fields failed validation.");

        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", violations);

        return super.handleExceptionInternal(ex, problem, headers, status, request);
    }

    public record FieldViolation(String field, String detail) {}
}
