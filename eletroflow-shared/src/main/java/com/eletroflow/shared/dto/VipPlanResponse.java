package com.eletroflow.shared.dto;

import java.math.BigDecimal;

public record VipPlanResponse(
        String key,
        String displayName,
        BigDecimal amount,
        String currency,
        String discordRoleId,
        int durationDays
) {
}
