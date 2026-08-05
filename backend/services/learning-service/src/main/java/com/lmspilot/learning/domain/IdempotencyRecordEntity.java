package com.lmspilot.learning.domain; import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="idempotency_records") public class IdempotencyRecordEntity { @Id @Column(length=160) public String idempotencyKey=""; @Column(nullable=false) public Instant createdAt=Instant.now(); public IdempotencyRecordEntity(){} }
