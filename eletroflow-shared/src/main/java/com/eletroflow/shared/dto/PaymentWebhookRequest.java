package com.eletroflow.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentWebhookRequest(
        @NotBlank String txid,
        @NotBlank String endToEndId,
        @NotNull BigDecimal amount,
        String payerDocument,
        @NotNull OffsetDateTime paidAt
) {
}
