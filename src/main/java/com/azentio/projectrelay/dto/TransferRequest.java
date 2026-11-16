package com.azentio.projectrelay.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferRequest(
        @NotBlank @Size(max = 100) String transferId,
        @NotBlank @Pattern(regexp = "\\d{10,16}") String fromAccount,
        @NotBlank @Pattern(regexp = "\\d{10,16}") String toAccount,
        @NotNull @DecimalMin("0.01") @DecimalMax("10000000.00")
        @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull OffsetDateTime occurredAt) {

    @AssertTrue(message = "fromAccount and toAccount must differ")
    public boolean isAccountsDifferent() {
        return fromAccount == null || toAccount == null || !fromAccount.equals(toAccount);
    }
}
