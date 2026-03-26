package com.eletroflow.plugin.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentCreation(
        String id,
        String userId,
        String planKey,
        BigDecimal amount,
        String txid,
        String payerName,
        String payerCpf,
        String copyPasteCode,
        String qrCodeBase64,
        String qrCodeUrl,
        String discordThreadId,
        OffsetDateTime expiresAt
) {
}
