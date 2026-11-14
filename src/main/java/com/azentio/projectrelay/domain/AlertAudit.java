package com.azentio.projectrelay.domain;
import jakarta.persistence.*;import java.time.OffsetDateTime;
@Entity @Table(name="alert_audit") public class AlertAudit{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="audit_id") public Long auditId;
 @Column(name="alert_id") public Long alertId; @Column(name="from_status") public String fromStatus; @Column(name="to_status") public String toStatus;
 public String actor; public String note; @Column(name="created_at") public OffsetDateTime createdAt;
}