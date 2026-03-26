package com.eletroflow.plugin.model;

import java.math.BigDecimal;

public record PlanRecord(
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

