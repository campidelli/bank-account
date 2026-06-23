package com.bank.account.adapter.in.web.validation;

import com.modernmt.text.profanity.ProfanityFilter;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotOffensiveNickNameValidator implements ConstraintValidator<NotOffensiveNickName, String> {

    private static final ProfanityFilter FILTER = new ProfanityFilter();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return !FILTER.test("en", value);
    }
}
