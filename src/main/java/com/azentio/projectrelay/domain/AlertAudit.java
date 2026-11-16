package com.azentio.projectrelay.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alert_audit")
public class AlertAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    public Long auditId;

    @Column(name = "alert_id")
    public Long alertId;

    @Column(name = "from_status", length = 20)
    public String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    public String toStatus;

    @Column(length = 100)
    public String actor;

    @Column(columnDefinition = "TEXT")
    public String note;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
