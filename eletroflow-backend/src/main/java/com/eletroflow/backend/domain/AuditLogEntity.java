package com.eletroflow.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aggregate_type", nullable = false, length = 40)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 1500)
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
