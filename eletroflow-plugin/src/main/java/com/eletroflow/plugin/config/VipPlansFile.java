package com.eletroflow.plugin.config;

import java.math.BigDecimal;
import java.util.Map;

public record VipPlansFile(Map<String, VipPlanNode> plans) {

    public record VipPlanNode(
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
}

