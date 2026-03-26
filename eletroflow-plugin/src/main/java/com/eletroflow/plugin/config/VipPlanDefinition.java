package com.eletroflow.plugin.config;

import java.math.BigDecimal;

public record VipPlanDefinition(
        String key,
        String displayName,
        BigDecimal amount,
        String currency,
        String luckPermsGroup,
        String discordRoleId,
        int durationDays,
        boolean active,
        int sortOrder
) {
}

