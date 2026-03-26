package com.eletroflow.plugin.model;

import com.eletroflow.shared.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentRecord(
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
        PaymentStatus status,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime rewardedAt
) {
}

