package com.bankpro.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("integrity errors are logged by constraint name only, never with the duplicated customer data")
    void constraintNameHidesValues() {
        var mysql = new SQLIntegrityConstraintViolationException(
                "Duplicate entry '+8801712345678' for key 'customers.uk_customers_phone'");
        String logged = GlobalExceptionHandler.constraintName(new DataIntegrityViolationException("x", mysql));
        assertThat(logged).isEqualTo("customers.uk_customers_phone").doesNotContain("8801712345678");

        var other = new IllegalStateException("something without a key name");
        assertThat(GlobalExceptionHandler.constraintName(new DataIntegrityViolationException("x", other)))
                .isEqualTo("IllegalStateException");
    }
}
