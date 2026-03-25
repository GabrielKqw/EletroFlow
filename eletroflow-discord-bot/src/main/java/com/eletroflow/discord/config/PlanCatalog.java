package com.eletroflow.discord.config;

import java.util.Map;

public record PlanCatalog(Map<String, PlanDefinition> plans) {

    public record PlanDefinition(
            String displayName,
            String discordRoleId,
            String description
    ) {
    }
}
