package com.eletroflow.shared.dto;

import com.eletroflow.shared.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        String paymentId,
        String txid,
        PaymentStatus status,
        String planKey,
        BigDecimal amount,
        String qrCodeBase64,
        String copyPasteCode,
        OffsetDateTime expiresAt
) {
}
