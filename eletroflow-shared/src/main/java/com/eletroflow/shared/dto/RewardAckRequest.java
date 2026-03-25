package com.eletroflow.shared.dto;

import jakarta.validation.constraints.NotBlank;

public record RewardAckRequest(
        @NotBlank String serverId,
        @NotBlank String externalReference,
        String failureReason
) {
}
