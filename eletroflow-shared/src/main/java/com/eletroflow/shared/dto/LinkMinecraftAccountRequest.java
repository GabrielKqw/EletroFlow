package com.eletroflow.shared.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkMinecraftAccountRequest(
        @NotBlank String discordId,
        @NotBlank String minecraftUuid,
        @NotBlank String minecraftUsername
) {
}
