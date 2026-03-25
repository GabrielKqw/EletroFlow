package com.eletroflow.backend.config;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eletroflow.catalog")
public record PlanCatalogProperties(Map<String, PlanDefinition> plans) {

    public record PlanDefinition(
            String displayName,
            BigDecimal amount,
            String currency,
            String luckPermsGroup,
            String discordRoleId,
            int durationDays
    ) {
    }
}
