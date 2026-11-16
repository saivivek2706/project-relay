package com.azentio.projectrelay.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {
    @Id
    @Column(name = "transfer_id", length = 100)
    public String transferId;

    @Column(name = "from_account", nullable = false, length = 16)
    public String fromAccount;

    @Column(name = "to_account", nullable = false, length = 16)
    public String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    public OffsetDateTime occurredAt;

    public Transfer() { }

    public Transfer(String id, String from, String to, BigDecimal amount, OffsetDateTime occurredAt) {
        this.transferId = id;
        this.fromAccount = from;
        this.toAccount = to;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }
}
