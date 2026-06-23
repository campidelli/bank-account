package com.bank.account.adapter.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotOffensiveNickNameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotOffensiveNickName {

    String message() default "Account nick name contains offensive language";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
