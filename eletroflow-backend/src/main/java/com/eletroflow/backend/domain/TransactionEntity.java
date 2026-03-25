package com.eletroflow.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "provider_event_id", nullable = false, unique = true, length = 100)
    private String providerEventId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payer_document", length = 40)
    private String payerDocument;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setProviderEventId(String providerEventId) {
        this.providerEventId = providerEventId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setPayerDocument(String payerDocument) {
        this.payerDocument = payerDocument;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
