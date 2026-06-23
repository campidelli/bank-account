package com.bank.account.adapter.in.web.validation;

import com.bank.account.adapter.in.web.dto.CreateAccountRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequest_noViolations() {
        var request = new CreateAccountRequest("03", "0473", "John Smith", "MySavings");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void missingCustomerName_hasViolation() {
        var request = new CreateAccountRequest("03", "0473", null, "MySavings");
        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("customerName");
    }

    @Test
    void nullNickName_noViolation() {
        assertThat(validator.validate(new CreateAccountRequest("03", "0473", "John Smith", null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "ab"})
    void nickNameTooShort_hasViolation(String nickName) {
        Set<ConstraintViolation<CreateAccountRequest>> violations =
                validator.validate(new CreateAccountRequest("03", "0473", "John Smith", nickName));
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNickName"))).isTrue();
    }

    @Test
    void nickNameTooLong_hasViolation() {
        Set<ConstraintViolation<CreateAccountRequest>> violations =
                validator.validate(new CreateAccountRequest("03", "0473", "John Smith", "a".repeat(31)));
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"fucker", "bastard", "dickhead"})
    void offensiveNickName_hasViolation(String nickName) {
        Set<ConstraintViolation<CreateAccountRequest>> violations =
                validator.validate(new CreateAccountRequest("03", "0473", "John Smith", nickName));
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNickName"))).isTrue();
    }
}
