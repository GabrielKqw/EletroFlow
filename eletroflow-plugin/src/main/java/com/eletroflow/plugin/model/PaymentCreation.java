package com.eletroflow.plugin.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentCreation(
        String id,
        String discordId,
        String minecraftUuid,
        String minecraftUsername,
        String planKey,
        BigDecimal amount,
        String txid,
        String copyPasteCode,
        String qrCodeBase64,
        String discordThreadId,
        OffsetDateTime expiresAt
) {
}

