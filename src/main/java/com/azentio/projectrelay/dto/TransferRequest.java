package com.azentio.projectrelay.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.OffsetDateTime;
public record TransferRequest(@NotBlank @Size(max=100) String transferId,@NotBlank @Pattern(regexp="\\d{10,16}") String fromAccount,@NotBlank @Pattern(regexp="\\d{10,16}") String toAccount,@NotNull @DecimalMin(value="0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@NotNull OffsetDateTime occurredAt){
 public TransferRequest{ if(fromAccount!=null && fromAccount.equals(toAccount)) throw new IllegalArgumentException("fromAccount must differ from toAccount"); if(amount!=null && amount.compareTo(new java.math.BigDecimal("10000000.00"))>0) throw new IllegalArgumentException("amount exceeds max"); }
}