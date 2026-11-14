package com.azentio.projectrelay.domain;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.OffsetDateTime;
@Entity @Table(name="transfers") public class Transfer{
 @Id @Column(name="transfer_id") public String transferId;
 @Column(name="from_account",nullable=false) public String fromAccount;
 @Column(name="to_account",nullable=false) public String toAccount;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal amount;
 @Column(name="occurred_at",nullable=false) public OffsetDateTime occurredAt;
 public Transfer(){} public Transfer(String id,String f,String t,BigDecimal a,OffsetDateTime o){transferId=id;fromAccount=f;toAccount=t;amount=a;occurredAt=o;}
}