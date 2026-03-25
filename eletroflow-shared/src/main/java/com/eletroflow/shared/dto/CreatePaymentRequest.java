package com.eletroflow.shared.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(
        @NotBlank String discordId,
        @NotBlank String planKey,
        String minecraftUuid,
        String minecraftUsername,
        String ticketChannelId
) {
}
