package com.eletroflow.shared.dto;

import com.eletroflow.shared.enums.ProvisionStatus;
import java.time.OffsetDateTime;

public record PendingRewardResponse(
        String rewardId,
        String paymentId,
        String discordId,
        String minecraftUuid,
        String minecraftUsername,
        String planKey,
        String luckPermsGroup,
        String discordRoleId,
        ProvisionStatus status,
        OffsetDateTime createdAt
) {
}
