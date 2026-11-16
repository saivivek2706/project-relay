package com.azentio.projectrelay.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    public Long alertId;

    @Column(nullable = false)
    public int hops;

    @Column(name = "first_amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal firstAmount;

    @Column(name = "last_amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal lastAmount;

    @Column(name = "end_to_end_retention", nullable = false, precision = 10, scale = 4)
    public BigDecimal endToEndRetention;

    @Column(name = "start_at", nullable = false)
    public OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    public OffsetDateTime endAt;

    @Column(nullable = false, length = 20)
    public String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @Column(name = "chain_transfer_ids", nullable = false, columnDefinition = "TEXT")
    public String chainTransferIds;

    public List<String> chainIds() {
        return chainTransferIds == null || chainTransferIds.isBlank()
                ? List.of() : Arrays.asList(chainTransferIds.split(","));
    }

    public void setChainIds(List<String> ids) {
        chainTransferIds = String.join(",", ids);
    }
}
