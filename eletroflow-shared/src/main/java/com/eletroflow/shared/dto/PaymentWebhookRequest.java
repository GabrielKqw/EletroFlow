package com.eletroflow.shared.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentWebhookRequest(
        String txid,
        String endToEndId,
        BigDecimal amount,
        String payerDocument,
        OffsetDateTime paidAt
) {
}
